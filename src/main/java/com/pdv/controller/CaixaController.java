package com.pdv.controller;

import com.pdv.dao.ConexaoSQLite;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class CaixaController {

    @FXML private Label lblStatusCaixa;
    @FXML private VBox boxAbrirCaixa;
    @FXML private ScrollPane boxOperacaoCaixa; // CORRIGIDO PARA SCROLLPANE PARA EVITAR ERRO DE FXML
    @FXML private Label lblMensagem;

    // Abertura
    @FXML private TextField txtValorAbertura;

    // Operações (Sangria/Suprimento)
    @FXML private ComboBox<String> cbTipoMovimento;
    @FXML private TextField txtValorMovimento;
    @FXML private TextField txtMotivoMovimento;

    // Fechamento e Resumo
    @FXML private Label lblResumo;
    @FXML private Label lblTotalGaveta;
    @FXML private TextField txtValorFechamento;

    private int turnoAtualId = -1;
    private double valorEsperadoGaveta = 0.0;

    @FXML
    public void initialize() {
        cbTipoMovimento.setItems(FXCollections.observableArrayList("SANGRIA (Retirada)", "SUPRIMENTO (Entrada)"));
        cbTipoMovimento.setValue("SANGRIA (Retirada)");

        // Máscaras de dinheiro
        aplicarMascaraDinheiro(txtValorAbertura);
        aplicarMascaraDinheiro(txtValorMovimento);
        aplicarMascaraDinheiro(txtValorFechamento);

        Platform.runLater(this::verificarStatusCaixa);
    }

    private void aplicarMascaraDinheiro(TextField campo) {
        if (campo != null) {
            campo.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*(\\.\\d*)?")) {
                    campo.setText(newVal.replaceAll("[^\\d.]", ""));
                }
            });
        }
    }

    private void verificarStatusCaixa() {
        String sql = "SELECT id, valor_abertura FROM Caixa_Turnos WHERE status = 'ABERTO' ORDER BY id DESC LIMIT 1";
        
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                turnoAtualId = rs.getInt("id");
                
                lblStatusCaixa.setText("STATUS: CAIXA ABERTO (Turno #" + turnoAtualId + ")");
                lblStatusCaixa.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2ed573;");
                
                boxAbrirCaixa.setVisible(false);
                boxAbrirCaixa.setManaged(false);
                
                boxOperacaoCaixa.setVisible(true);
                boxOperacaoCaixa.setManaged(true);
                
                calcularResumoTurno();
            } else {
                turnoAtualId = -1;
                lblStatusCaixa.setText("STATUS: CAIXA FECHADO");
                lblStatusCaixa.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ff5252;");
                
                boxAbrirCaixa.setVisible(true);
                boxAbrirCaixa.setManaged(true);
                
                boxOperacaoCaixa.setVisible(false);
                boxOperacaoCaixa.setManaged(false);
            }
        } catch (Exception e) {
            mostrarAviso("Erro ao verificar caixa: " + e.getMessage());
        }
    }

    @FXML
    public void abrirCaixa(ActionEvent event) {
        String valorStr = txtValorAbertura.getText().replace(",", ".");
        if (valorStr.isEmpty()) {
            mostrarAviso("Informe o valor inicial (troco). Se for zero, digite 0.");
            return;
        }

        try {
            double valor = Double.parseDouble(valorStr);
            String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            String sql = "INSERT INTO Caixa_Turnos (data_abertura, valor_abertura, status) VALUES (?, ?, 'ABERTO')";
            
            try (Connection conn = ConexaoSQLite.conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, dataHora);
                stmt.setDouble(2, valor);
                stmt.executeUpdate();
                
                mostrarSucesso("Caixa aberto com sucesso!");
                txtValorAbertura.clear();
                verificarStatusCaixa(); 
            }
        } catch (Exception e) {
            mostrarAviso("Erro ao abrir caixa: " + e.getMessage());
        }
    }

    @FXML
    public void registrarMovimento(ActionEvent event) {
        if (turnoAtualId == -1) return;
        
        String valorStr = txtValorMovimento.getText().replace(",", ".");
        String motivo = txtMotivoMovimento.getText().trim();
        
        if (valorStr.isEmpty() || motivo.isEmpty()) {
            mostrarAviso("Preencha o valor e o motivo da movimentação.");
            return;
        }
        
        try {
            double valor = Double.parseDouble(valorStr);
            if (valor <= 0) throw new NumberFormatException();
            
            String tipo = cbTipoMovimento.getValue().contains("SANGRIA") ? "SANGRIA" : "SUPRIMENTO";
            String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            String sql = "INSERT INTO Caixa_Movimentos (turno_id, tipo, valor, descricao, data_movimento) VALUES (?, ?, ?, ?, ?)";
            
            try (Connection conn = ConexaoSQLite.conectar();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, turnoAtualId);
                stmt.setString(2, tipo);
                stmt.setDouble(3, valor);
                stmt.setString(4, motivo);
                stmt.setString(5, dataHora);
                stmt.executeUpdate();
                
                mostrarSucesso(tipo + " registrada com sucesso!");
                txtValorMovimento.clear();
                txtMotivoMovimento.clear();
                calcularResumoTurno();
            }
        } catch (Exception e) {
            mostrarAviso("Valor inválido.");
        }
    }

    private void calcularResumoTurno() {
        if (turnoAtualId == -1) return;
        
        double abertura = 0.0, entradas = 0.0, saidas = 0.0, vendasDinheiro = 0.0;
        
        try (Connection conn = ConexaoSQLite.conectar()) {
            String sqlAbertura = "SELECT valor_abertura FROM Caixa_Turnos WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlAbertura)) {
                stmt.setInt(1, turnoAtualId);
                ResultSet rs = stmt.executeQuery();
                if(rs.next()) abertura = rs.getDouble("valor_abertura");
            }
            
            String sqlMov = "SELECT tipo, SUM(valor) as total FROM Caixa_Movimentos WHERE turno_id = ? GROUP BY tipo";
            try (PreparedStatement stmt = conn.prepareStatement(sqlMov)) {
                stmt.setInt(1, turnoAtualId);
                ResultSet rs = stmt.executeQuery();
                while(rs.next()) {
                    if (rs.getString("tipo").equals("SUPRIMENTO")) entradas = rs.getDouble("total");
                    if (rs.getString("tipo").equals("SANGRIA")) saidas = rs.getDouble("total");
                }
            }
            
            // Pega as Vendas em Dinheiro amarradas a este turno específico
            String sqlVendas = "SELECT SUM(total) as total_dinheiro FROM Vendas WHERE turno_id = ? AND forma_pagamento = 'Dinheiro' AND status = 'CONCLUIDA'";
            try (PreparedStatement stmt = conn.prepareStatement(sqlVendas)) {
                stmt.setInt(1, turnoAtualId);
                ResultSet rs = stmt.executeQuery();
                if(rs.next()) vendasDinheiro = rs.getDouble("total_dinheiro");
            }
            
            valorEsperadoGaveta = (abertura + entradas + vendasDinheiro) - saidas;
            
            lblResumo.setText(String.format("Fundo Inicial: R$ %.2f | Vendas (Dinheiro): R$ %.2f | Suprimentos: R$ %.2f | Sangrias: R$ %.2f", 
                                            abertura, vendasDinheiro, entradas, saidas));
            lblTotalGaveta.setText(String.format("Valor Esperado na Gaveta: R$ %.2f", valorEsperadoGaveta));
            
        } catch (Exception e) {
            mostrarAviso("Erro ao calcular resumo: " + e.getMessage());
        }
    }

    @FXML
    public void fecharCaixa(ActionEvent event) {
        if (turnoAtualId == -1) return;
        
        String valorFechamentoStr = txtValorFechamento.getText().replace(",", ".");
        if (valorFechamentoStr.isEmpty()) {
            mostrarAviso("Informe quanto de dinheiro FÍSICO tem na gaveta agora.");
            return;
        }
        
        try {
            double valorFechamento = Double.parseDouble(valorFechamentoStr);
            double diferenca = valorFechamento - valorEsperadoGaveta;
            
            String msgConfirmacao = "O sistema espera R$ " + String.format("%.2f", valorEsperadoGaveta) + 
                                    "\nVocê informou R$ " + String.format("%.2f", valorFechamento) + 
                                    "\nDiferença: R$ " + String.format("%.2f", diferenca) + 
                                    "\n\nDeseja confirmar o fechamento do caixa?";
                                    
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmação de Fechamento");
            alert.setHeaderText("Atenção - Fechamento de Turno");
            alert.setContentText(msgConfirmacao);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                
                String dataHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                String sql = "UPDATE Caixa_Turnos SET data_fechamento = ?, valor_fechamento = ?, status = 'FECHADO' WHERE id = ?";
                
                try (Connection conn = ConexaoSQLite.conectar();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    
                    stmt.setString(1, dataHora);
                    stmt.setDouble(2, valorFechamento);
                    stmt.setInt(3, turnoAtualId);
                    stmt.executeUpdate();
                    
                    mostrarSucesso("Caixa FECHADO com sucesso!");
                    txtValorFechamento.clear();
                    verificarStatusCaixa(); 
                }
            }
        } catch (Exception e) {
            mostrarAviso("Valor de fechamento inválido.");
        }
    }

    private void mostrarSucesso(String msg) {
        lblMensagem.setStyle("-fx-text-fill: #2ed573; -fx-font-weight: bold;");
        lblMensagem.setText(msg);
    }

    private void mostrarAviso(String msg) {
        lblMensagem.setStyle("-fx-text-fill: #ffce00; -fx-font-weight: bold;");
        lblMensagem.setText(msg);
    }
}