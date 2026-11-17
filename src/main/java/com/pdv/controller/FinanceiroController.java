package com.pdv.controller;

import com.pdv.dao.ConexaoSQLite;
import com.pdv.model.Transacao;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Optional;

public class FinanceiroController {

    @FXML private Label lblTotalEntradas;
    @FXML private Label lblTotalSaidas;
    @FXML private Label lblSaldo;

    @FXML private ComboBox<String> cbTipoTransacao;
    @FXML private TextField txtCategoria;
    @FXML private TextField txtDescricao;
    @FXML private TextField txtValor;
    @FXML private DatePicker dpVencimento;
    @FXML private ComboBox<String> cbStatus;
    @FXML private Label lblStatusAviso;

    @FXML private ComboBox<String> cbFiltroStatus;
    @FXML private TableView<Transacao> tabelaFinanceiro;
    @FXML private TableColumn<Transacao, String> colData;
    @FXML private TableColumn<Transacao, String> colTipo;
    @FXML private TableColumn<Transacao, String> colDescricao;
    @FXML private TableColumn<Transacao, String> colValor;
    
    // Nível 2
    @FXML private TableColumn<Transacao, String> colPago;
    
    @FXML private TableColumn<Transacao, String> colStatus;

    private ObservableList<Transacao> listaTransacoes = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cbTipoTransacao.setItems(FXCollections.observableArrayList("SAIDA", "ENTRADA"));
        cbTipoTransacao.setValue("SAIDA"); 

        cbStatus.setItems(FXCollections.observableArrayList("PAGO", "PENDENTE"));
        cbStatus.setValue("PAGO");

        cbFiltroStatus.setItems(FXCollections.observableArrayList("Todos", "Apenas PENDENTES", "Apenas PAGOS"));
        cbFiltroStatus.setValue("Todos");

        dpVencimento.setValue(LocalDate.now());

        // Máscara para dinheiro
        txtValor.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtValor.setText(newVal.replaceAll("[^\\d.]", ""));
            }
        });

        colData.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDataVencimento()));
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipo()));
        colDescricao.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescricao()));
        colValor.setCellValueFactory(data -> new SimpleStringProperty(String.format("R$ %.2f", data.getValue().getValor())));
        
        // Nível 2
        colPago.setCellValueFactory(data -> new SimpleStringProperty(String.format("R$ %.2f", data.getValue().getValorPago())));
        
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        tabelaFinanceiro.setItems(listaTransacoes);

        carregarTabela();
        calcularResumoCaixa();
    }

    @FXML
    public void carregarTabela() {
        listaTransacoes.clear();
        String filtro = cbFiltroStatus.getValue();
        
        // Adicionadas as colunas valor_pago, data_pagamento e cliente_id do Nível 2
        String sql = "SELECT id, tipo, categoria, descricao, valor, valor_pago, data_vencimento, data_pagamento, status, cliente_id FROM Financeiro";
        
        if (filtro.equals("Apenas PENDENTES")) sql += " WHERE status = 'PENDENTE'";
        else if (filtro.equals("Apenas PAGOS")) sql += " WHERE status = 'PAGO'";

        sql += " ORDER BY data_vencimento DESC";

        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                listaTransacoes.add(new Transacao(
                        rs.getInt("id"),
                        rs.getString("tipo"),
                        rs.getString("categoria"),
                        rs.getString("descricao"),
                        rs.getDouble("valor"),
                        rs.getDouble("valor_pago"),
                        rs.getString("data_vencimento"),
                        rs.getString("data_pagamento"),
                        rs.getString("status"),
                        rs.getInt("cliente_id")
                ));
            }
        } catch (Exception e) {
            lblStatusAviso.setText("Erro ao carregar contas: " + e.getMessage());
        }
    }

    private void calcularResumoCaixa() {
        // Agora o resumo é baseado no que realmente foi pago (valor_pago)
        String sql = "SELECT tipo, SUM(valor_pago) as total FROM Financeiro GROUP BY tipo";
        double entradas = 0.0, saidas = 0.0;

        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                if (rs.getString("tipo").equals("ENTRADA")) entradas = rs.getDouble("total");
                if (rs.getString("tipo").equals("SAIDA")) saidas = rs.getDouble("total");
            }

            lblTotalEntradas.setText(String.format("R$ %.2f", entradas));
            lblTotalSaidas.setText(String.format("R$ %.2f", saidas));
            lblSaldo.setText(String.format("R$ %.2f", (entradas - saidas)));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void registrarTransacao(ActionEvent event) {
        String categoria = txtCategoria.getText().trim();
        String descricao = txtDescricao.getText().trim();
        LocalDate data = dpVencimento.getValue();

        if (categoria.isEmpty() || descricao.isEmpty() || data == null) {
            lblStatusAviso.setStyle("-fx-text-fill: #ffce00;");
            lblStatusAviso.setText("Preencha categoria, descrição e data.");
            return;
        }

        try {
            double valor = Double.parseDouble(txtValor.getText().replace(",", "."));
            if (valor <= 0) throw new NumberFormatException();

            // Nível 2
            String statusInicial = cbStatus.getValue();
            double valorPagoInicial = statusInicial.equals("PAGO") ? valor : 0.0;

            String sql = "INSERT INTO Financeiro (tipo, categoria, descricao, valor, valor_pago, data_vencimento, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection conn = ConexaoSQLite.conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, cbTipoTransacao.getValue());
                stmt.setString(2, categoria);
                stmt.setString(3, descricao);
                stmt.setDouble(4, valor);
                stmt.setDouble(5, valorPagoInicial);
                stmt.setString(6, data.toString());
                stmt.setString(7, statusInicial);
                
                if(statusInicial.equals("PAGO")){
                    stmt.executeUpdate();
                    String update = "UPDATE Financeiro SET data_pagamento = CURRENT_DATE WHERE id = last_insert_rowid()";
                    conn.createStatement().execute(update);
                } else {
                    stmt.executeUpdate();
                }

                txtCategoria.clear();
                txtDescricao.clear();
                txtValor.clear();
                lblStatusAviso.setStyle("-fx-text-fill: #2ed573;");
                lblStatusAviso.setText("Transação salva!");
                
                carregarTabela();
                calcularResumoCaixa();
            }

        } catch (NumberFormatException e) {
            lblStatusAviso.setStyle("-fx-text-fill: #ff5252;");
            lblStatusAviso.setText("Valor inválido.");
        } catch (Exception e) {
            lblStatusAviso.setStyle("-fx-text-fill: #ff5252;");
            lblStatusAviso.setText("Erro: " + e.getMessage());
        }
    }

    // =========================================================================
    // NÍVEL 2: BAIXA PARCIAL E LIBERAÇÃO DE LIMITE DO CLIENTE
    // =========================================================================
    @FXML
    public void marcarComoPago(ActionEvent event) {
        Transacao selecionada = tabelaFinanceiro.getSelectionModel().getSelectedItem();
        
        if (selecionada == null) {
            lblStatusAviso.setStyle("-fx-text-fill: #ffce00;");
            lblStatusAviso.setText("Selecione uma conta na tabela.");
            return;
        }
        if (!"PENDENTE".equals(selecionada.getStatus())) {
            lblStatusAviso.setStyle("-fx-text-fill: #ffce00;");
            lblStatusAviso.setText("Esta conta já está PAGA ou CANCELADA.");
            return;
        }

        double devedor = selecionada.getSaldoDevedor();

        // Abre um Pop-up perguntando qual é o valor que está sendo pago agora
        TextInputDialog dialog = new TextInputDialog(String.format("%.2f", devedor).replace(",", "."));
        dialog.setTitle("Baixa Parcial ou Total");
        dialog.setHeaderText("Conta: " + selecionada.getDescricao() + "\nFalta Receber/Pagar: R$ " + String.format("%.2f", devedor));
        dialog.setContentText("Valor sendo pago hoje (R$):");
        
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/view/estilo.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(valorStr -> {
            try {
                double valorDigitado = Double.parseDouble(valorStr.replace(",", "."));
                if (valorDigitado <= 0) {
                    lblStatusAviso.setStyle("-fx-text-fill: #ffce00;");
                    lblStatusAviso.setText("O valor deve ser maior que zero.");
                    return;
                }
                
                // Evita pagar mais do que deve
                if (valorDigitado > devedor) {
                    valorDigitado = devedor;
                }

                double novoValorPago = selecionada.getValorPago() + valorDigitado;
                String novoStatus = (novoValorPago >= selecionada.getValor()) ? "PAGO" : "PENDENTE";

                try (Connection conn = ConexaoSQLite.conectar()) {
                    conn.setAutoCommit(false);
                    
                    try {
                        // 1. Atualiza o pagamento na conta
                        String sqlFin = "UPDATE Financeiro SET valor_pago = ?, status = ?, data_pagamento = CURRENT_DATE WHERE id = ?";
                        try (PreparedStatement stmtFin = conn.prepareStatement(sqlFin)) {
                            stmtFin.setDouble(1, novoValorPago);
                            stmtFin.setString(2, novoStatus);
                            stmtFin.setInt(3, selecionada.getId());
                            stmtFin.executeUpdate();
                        }
                        
                        // 2. Se for um Fiado, libera o Limite do Cliente
                        if (selecionada.getClienteId() > 0 && "Fiado / Caderninho".equals(selecionada.getCategoria())) {
                            String sqlCli = "UPDATE Clientes SET credito_usado = credito_usado - ? WHERE id = ?";
                            try (PreparedStatement stmtCli = conn.prepareStatement(sqlCli)) {
                                stmtCli.setDouble(1, valorDigitado);
                                stmtCli.setInt(2, selecionada.getClienteId());
                                stmtCli.executeUpdate();
                            }
                        }
                        
                        conn.commit();
                        
                        lblStatusAviso.setStyle("-fx-text-fill: #2ed573;");
                        lblStatusAviso.setText("Pagamento de R$ " + String.format("%.2f", valorDigitado) + " registrado!");
                        
                        carregarTabela();
                        calcularResumoCaixa();
                        
                    } catch (Exception e) {
                        conn.rollback();
                        lblStatusAviso.setStyle("-fx-text-fill: #ff5252;");
                        lblStatusAviso.setText("Erro ao processar: " + e.getMessage());
                    } finally {
                        conn.setAutoCommit(true);
                    }
                }
            } catch (Exception e) {
                lblStatusAviso.setStyle("-fx-text-fill: #ff5252;");
                lblStatusAviso.setText("Valor inválido.");
            }
        });
    }

    @FXML
    public void excluirTransacao(ActionEvent event) {
        Transacao selecionada = tabelaFinanceiro.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            lblStatusAviso.setStyle("-fx-text-fill: #ffce00;");
            lblStatusAviso.setText("Selecione um item para excluir.");
            return;
        }

        if (selecionada.getClienteId() > 0) {
            lblStatusAviso.setStyle("-fx-text-fill: #ffce00;");
            lblStatusAviso.setText("Você não pode excluir uma venda de Fiado por aqui. Use o Cancelamento.");
            return;
        }

        String sql = "DELETE FROM Financeiro WHERE id = ?";
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, selecionada.getId());
            stmt.executeUpdate();
            
            lblStatusAviso.setStyle("-fx-text-fill: #2ed573;");
            lblStatusAviso.setText("Transação apagada.");
            
            carregarTabela();
            calcularResumoCaixa();
        } catch (Exception e) {
            lblStatusAviso.setText("Erro: " + e.getMessage());
        }
    }
}