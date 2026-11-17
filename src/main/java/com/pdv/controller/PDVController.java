package com.pdv.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPRow;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pdv.dao.ConexaoSQLite;
import com.pdv.model.ItemVenda;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

class VendaEstorno {
    int id;
    String data, pagamento, status;
    double total;

    public VendaEstorno(int id, String data, double total, String pag, String status) {
        this.id = id;
        this.data = data;
        this.total = total;
        this.pagamento = pag;
        this.status = status;
    }
}

public class PDVController {

    @FXML private StackPane rootStackPane;
    @FXML private VBox rootPane;

    @FXML private TextField txtBuscaProduto;
    @FXML private ListView<String> listaSugestoes;
    @FXML private TextField txtQuantidade;
    @FXML private Label lblStatus;
    
    @FXML private Label lblSubtotal;
    @FXML private TextField txtDesconto;
    @FXML private Label lblTotal;
    
    @FXML private ComboBox<String> cbFormaPagamento;
    @FXML private VBox boxFiado;
    
    @FXML private TextField txtBuscaCliente;
    @FXML private ListView<String> listaSugestoesCliente;
    @FXML private DatePicker dpVencimentoFiado;
    @FXML private TextField txtJurosFiado; 

    @FXML private TableView<ItemVenda> tabelaCarrinho;
    @FXML private TableColumn<ItemVenda, String> colCodigo;
    @FXML private TableColumn<ItemVenda, String> colNome;
    @FXML private TableColumn<ItemVenda, Number> colQtd;
    @FXML private TableColumn<ItemVenda, String> colUnitario;
    @FXML private TableColumn<ItemVenda, String> colSubtotal;

    @FXML private VBox painelEstorno;
    @FXML private TextField txtBuscaEstorno;
    @FXML private TableView<VendaEstorno> tabelaEstorno;
    @FXML private TableColumn<VendaEstorno, String> colEstornoId;
    @FXML private TableColumn<VendaEstorno, String> colEstornoData;
    @FXML private TableColumn<VendaEstorno, String> colEstornoTotal;
    @FXML private TableColumn<VendaEstorno, String> colEstornoPag;
    @FXML private TableColumn<VendaEstorno, String> colEstornoStatus;
    @FXML private Label lblStatusEstorno;
    
    private ObservableList<VendaEstorno> listaEstorno = FXCollections.observableArrayList();

    private ObservableList<ItemVenda> listaItens = FXCollections.observableArrayList();
    private double subtotalVenda = 0.0;
    private double totalVenda = 0.0;
    
    private int produtoSelecionadoAutocompletar = -1;
    private int clienteSelecionadoFiado = -1;

    @FXML
    public void initialize() {
        cbFormaPagamento.setItems(FXCollections.observableArrayList("Dinheiro", "PIX", "Cartão de Crédito", "Cartão de Débito", "Fiado (Caderninho)"));
        cbFormaPagamento.setValue("Dinheiro");

        cbFormaPagamento.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isFiado = "Fiado (Caderninho)".equals(newVal);
            boxFiado.setVisible(isFiado);
            boxFiado.setManaged(isFiado);
            if(isFiado) {
                dpVencimentoFiado.setValue(LocalDate.now().plusDays(30));
            }
            atualizarTotal(); 
        });

        colCodigo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCodigo()));
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNome()));
        colQtd.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getQuantidade()));
        colUnitario.setCellValueFactory(data -> new SimpleStringProperty(String.format("R$ %.2f", data.getValue().getPrecoUnitario())));
        colSubtotal.setCellValueFactory(data -> new SimpleStringProperty(String.format("R$ %.2f", data.getValue().getSubtotal())));
        tabelaCarrinho.setItems(listaItens);

        if (colEstornoId != null) {
            colEstornoId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().id)));
            colEstornoData.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().data));
            colEstornoTotal.setCellValueFactory(data -> new SimpleStringProperty(String.format("R$ %.2f", data.getValue().total)));
            colEstornoPag.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().pagamento));
            colEstornoStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status));
            
            colEstornoStatus.setCellFactory(column -> new TableCell<VendaEstorno, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item);
                        if (item.equals("CANCELADA")) {
                            setStyle("-fx-text-fill: #ff5252; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #2ed573; -fx-font-weight: bold;");
                        }
                    }
                }
            });
            tabelaEstorno.setItems(listaEstorno);
        }

        Platform.runLater(() -> {
            txtBuscaProduto.requestFocus();
            if (rootPane != null && rootPane.getScene() != null) {
                rootPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::escutarAtalhosGlobais);
            }
        });

        txtBuscaProduto.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                produtoSelecionadoAutocompletar = -1;
                listaSugestoes.setVisible(false);
                return;
            }
            if (produtoSelecionadoAutocompletar == -1) buscarSugestoes(newVal.trim());
        });

        listaSugestoes.setOnMouseClicked(event -> processarSelecaoSugestao());

        txtBuscaCliente.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                clienteSelecionadoFiado = -1;
                listaSugestoesCliente.setVisible(false);
                return;
            }
            if (clienteSelecionadoFiado == -1) buscarSugestoesCliente(newVal.trim());
        });
        
        listaSugestoesCliente.setOnMouseClicked(event -> processarSelecaoCliente());

        txtQuantidade.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtQuantidade.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
        
        txtDesconto.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtDesconto.setText(newVal.replaceAll("[^\\d.]", ""));
            }
            atualizarTotal();
        });
        
        if (txtJurosFiado != null) {
            txtJurosFiado.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!newVal.matches("\\d*(\\.\\d*)?")) {
                    txtJurosFiado.setText(newVal.replaceAll("[^\\d.]", ""));
                }
                atualizarTotal();
            });
        }
    }

    // ==========================================
    // MÉTODO AUXILIAR: VERIFICA SE HÁ CAIXA ABERTO
    // ==========================================
    private int obterTurnoAbertoId() {
        String sql = "SELECT id FROM Caixa_Turnos WHERE status = 'ABERTO' ORDER BY id DESC LIMIT 1";
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // Retorna -1 se não houver caixa aberto
    }

    private void escutarAtalhosGlobais(KeyEvent event) {
        if (painelEstorno != null && painelEstorno.isVisible()) {
            if (event.getCode() == KeyCode.ESCAPE) {
                fecharPainelEstorno(null);
            }
            return;
        }

        if (event.getCode() == KeyCode.F1) {
            txtBuscaProduto.requestFocus();
            event.consume();
        } else if (event.getCode() == KeyCode.F2) {
            cancelarVenda(null);
            event.consume();
        } else if (event.getCode() == KeyCode.F3) {
            txtDesconto.requestFocus();
            txtDesconto.selectAll();
            event.consume();
        } else if (event.getCode() == KeyCode.F4) {
            alterarQuantidadeItem(null);
            event.consume();
        } else if (event.getCode() == KeyCode.F5) { 
            abrirPainelEstorno(null);
            event.consume();
        } else if (event.getCode() == KeyCode.F12) {
            finalizarVenda(null);
            event.consume();
        } else if (event.getCode() == KeyCode.DELETE) {
            removerItem(null);
            event.consume();
        }
    }

    @FXML
    public void abrirPainelEstorno(ActionEvent event) {
        if (painelEstorno != null) {
            painelEstorno.setVisible(true);
            painelEstorno.toFront();
            txtBuscaEstorno.clear();
            carregarVendasEstorno(null);
            txtBuscaEstorno.requestFocus();
        }
    }
    
    @FXML
    public void fecharPainelEstorno(ActionEvent event) {
        if (painelEstorno != null) {
            painelEstorno.setVisible(false);
            txtBuscaProduto.requestFocus();
        }
    }
    
    @FXML
    public void carregarVendasEstorno(KeyEvent event) {
        listaEstorno.clear();
        String busca = txtBuscaEstorno.getText().trim();
        String sql = "SELECT id, data_venda, total, forma_pagamento, status FROM Vendas WHERE status = 'CONCLUIDA' OR status = 'CANCELADA' ";
        
        if (!busca.isEmpty()) {
            sql += "AND id = ? ";
        }
        sql += "ORDER BY id DESC LIMIT 50";
        
        try (Connection conn = ConexaoSQLite.conectar(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (!busca.isEmpty()) {
                stmt.setString(1, busca);
            }
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                listaEstorno.add(new VendaEstorno(
                        rs.getInt("id"), 
                        rs.getString("data_venda"), 
                        rs.getDouble("total"), 
                        rs.getString("forma_pagamento"), 
                        rs.getString("status")
                ));
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }
    
    @FXML
    public void confirmarEstorno(ActionEvent event) {
        VendaEstorno selecionada = tabelaEstorno.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            lblStatusEstorno.setStyle("-fx-text-fill: #ffce00;");
            lblStatusEstorno.setText("Selecione uma venda na tabela primeiro.");
            return;
        }
        if ("CANCELADA".equals(selecionada.status)) {
            lblStatusEstorno.setStyle("-fx-text-fill: #ffce00;");
            lblStatusEstorno.setText("Essa venda já foi estornada.");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Estorno");
        alert.setHeaderText("Você vai cancelar a Venda #" + selecionada.id);
        alert.setContentText("Isso vai devolver os itens ao estoque e cancelar a cobrança no Financeiro. Deseja continuar?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            executarRollbackDaVenda(selecionada.id);
        }
    }
    
    private void executarRollbackDaVenda(int idVenda) {
        try (Connection conn = ConexaoSQLite.conectar()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE Vendas SET status = 'CANCELADA' WHERE id = ?")) {
                    stmt.setInt(1, idVenda); 
                    stmt.executeUpdate();
                }
                
                try (PreparedStatement stmtItens = conn.prepareStatement("SELECT produto_id, quantidade FROM Itens_Venda WHERE venda_id = ?")) {
                    stmtItens.setInt(1, idVenda);
                    ResultSet rsItens = stmtItens.executeQuery();
                    
                    try (PreparedStatement stmtDevolve = conn.prepareStatement("UPDATE Produtos SET estoque_atual = estoque_atual + ? WHERE id = ?");
                         PreparedStatement stmtKardex = conn.prepareStatement("INSERT INTO Movimentacao_Estoque (produto_id, tipo, quantidade, motivo) VALUES (?, 'ENTRADA', ?, ?)")) {
                        
                        while (rsItens.next()) {
                            int pId = rsItens.getInt("produto_id");
                            int qtd = rsItens.getInt("quantidade");
                            
                            stmtDevolve.setInt(1, qtd); 
                            stmtDevolve.setInt(2, pId); 
                            stmtDevolve.addBatch();
                            
                            stmtKardex.setInt(1, pId); 
                            stmtKardex.setInt(2, qtd); 
                            stmtKardex.setString(3, "Estorno da Venda #" + idVenda); 
                            stmtKardex.addBatch();
                        }
                        stmtDevolve.executeBatch(); 
                        stmtKardex.executeBatch();
                    }
                }
                
                try (PreparedStatement stmtFin = conn.prepareStatement("SELECT id, categoria, cliente_id, valor FROM Financeiro WHERE descricao LIKE ?")) {
                    stmtFin.setString(1, "%Venda #" + idVenda + "%");
                    ResultSet rsFin = stmtFin.executeQuery();
                    
                    if (rsFin.next()) {
                        int fId = rsFin.getInt("id");
                        String cat = rsFin.getString("categoria");
                        int cId = rsFin.getInt("cliente_id");
                        double val = rsFin.getDouble("valor");
                        
                        try (PreparedStatement s = conn.prepareStatement("UPDATE Financeiro SET status = 'CANCELADO' WHERE id = ?")) {
                            s.setInt(1, fId); 
                            s.executeUpdate();
                        }
                        if (cId > 0 && "Fiado / Caderninho".equals(cat)) {
                            try (PreparedStatement s = conn.prepareStatement("UPDATE Clientes SET credito_usado = credito_usado - ? WHERE id = ?")) {
                                s.setDouble(1, val); 
                                s.setInt(2, cId); 
                                s.executeUpdate();
                            }
                        }
                    }
                }
                
                conn.commit();
                lblStatusEstorno.setStyle("-fx-text-fill: #2ed573;");
                lblStatusEstorno.setText("Venda #" + idVenda + " estornada com sucesso!");
                carregarVendasEstorno(null);
                
            } catch (Exception e) {
                conn.rollback();
                lblStatusEstorno.setStyle("-fx-text-fill: #ff5252;");
                lblStatusEstorno.setText("Erro ao estornar: " + e.getMessage());
            } finally { 
                conn.setAutoCommit(true); 
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private void buscarSugestoes(String busca) {
        if (busca.matches("\\d{6,}")) {
            listaSugestoes.setVisible(false);
            return; 
        }
        ObservableList<String> resultados = FXCollections.observableArrayList();
        String sql = "SELECT id, nome, preco_venda FROM Produtos WHERE ativo = 1 AND (nome LIKE ? OR codigo_barras LIKE ?) ORDER BY nome ASC";
        
        try (Connection conn = ConexaoSQLite.conectar(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + busca + "%");
            stmt.setString(2, busca + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                resultados.add(rs.getInt("id") + " - " + rs.getString("nome") + " | R$ " + String.format("%.2f", rs.getDouble("preco_venda")));
            }
            
            if (!resultados.isEmpty()) {
                listaSugestoes.setItems(resultados);
                listaSugestoes.toFront();
                listaSugestoes.setVisible(true);
            } else {
                listaSugestoes.setVisible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buscarSugestoesCliente(String busca) {
        ObservableList<String> resultados = FXCollections.observableArrayList();
        String sql = "SELECT id, nome, cpf FROM Clientes WHERE nome LIKE ? OR cpf LIKE ? ORDER BY nome ASC";
        
        try (Connection conn = ConexaoSQLite.conectar(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + busca + "%");
            stmt.setString(2, busca + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String cpfInfo = rs.getString("cpf") != null ? " | CPF: " + rs.getString("cpf") : "";
                resultados.add(rs.getInt("id") + " - " + rs.getString("nome") + cpfInfo);
            }
            
            if (!resultados.isEmpty()) {
                listaSugestoesCliente.setItems(resultados);
                listaSugestoesCliente.toFront();
                listaSugestoesCliente.setVisible(true);
            } else {
                listaSugestoesCliente.setVisible(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void navegarSugestoes(KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN && listaSugestoes.isVisible() && !listaSugestoes.getItems().isEmpty()) {
            listaSugestoes.requestFocus();
            listaSugestoes.getSelectionModel().selectFirst();
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER && !listaSugestoes.isVisible()) {
            adicionarItemFiltro(null);
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            listaSugestoes.setVisible(false);
        }
    }

    @FXML
    public void selecionarSugestaoTeclado(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            processarSelecaoSugestao();
            event.consume();
        } else if (event.getCode() == KeyCode.UP && listaSugestoes.getSelectionModel().getSelectedIndex() == 0) {
            txtBuscaProduto.requestFocus();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            listaSugestoes.setVisible(false);
            txtBuscaProduto.requestFocus();
        }
    }

    private void processarSelecaoSugestao() {
        String selecionado = listaSugestoes.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            String[] partes = selecionado.split("-", 2);
            if (partes.length > 1) {
                produtoSelecionadoAutocompletar = Integer.parseInt(partes[0].trim());
                txtBuscaProduto.setText(partes[1].split("\\|")[0].trim());
                listaSugestoes.setVisible(false);
                txtQuantidade.requestFocus();
            }
        }
    }

    @FXML
    public void enterNoQtd(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            adicionarItemFiltro(null);
            event.consume();
        }
    }

    @FXML
    public void navegarSugestoesCliente(KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN && listaSugestoesCliente.isVisible() && !listaSugestoesCliente.getItems().isEmpty()) {
            listaSugestoesCliente.requestFocus();
            listaSugestoesCliente.getSelectionModel().selectFirst();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            listaSugestoesCliente.setVisible(false);
        }
    }

    @FXML
    public void selecionarSugestaoClienteTeclado(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            processarSelecaoCliente();
            event.consume();
        } else if (event.getCode() == KeyCode.UP && listaSugestoesCliente.getSelectionModel().getSelectedIndex() == 0) {
            txtBuscaCliente.requestFocus();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            listaSugestoesCliente.setVisible(false);
            txtBuscaCliente.requestFocus();
        }
    }

    private void processarSelecaoCliente() {
        String selecionado = listaSugestoesCliente.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            String[] partes = selecionado.split("-", 2);
            if (partes.length > 1) {
                clienteSelecionadoFiado = Integer.parseInt(partes[0].trim());
                txtBuscaCliente.setText(partes[1].split("\\|")[0].trim());
                listaSugestoesCliente.setVisible(false);
            }
        }
    }

    @FXML
    public void adicionarItemFiltro(ActionEvent event) {
        // TRAVA DE SEGURANÇA: Impede lançar produtos se o caixa estiver fechado
        if (obterTurnoAbertoId() == -1) {
            mostrarAviso("ATENÇÃO: O Caixa está FECHADO! Abra o turno antes de iniciar vendas.");
            return;
        }

        String busca = txtBuscaProduto.getText().trim();
        if (busca.isEmpty()) return;

        int qtdSolicitada;
        try {
            qtdSolicitada = Integer.parseInt(txtQuantidade.getText().trim());
            if (qtdSolicitada <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            mostrarAviso("Quantidade inválida.");
            return;
        }

        String sql;
        if (produtoSelecionadoAutocompletar != -1) {
            sql = "SELECT id, codigo_barras, nome, preco_venda, estoque_atual FROM Produtos WHERE id = " + produtoSelecionadoAutocompletar;
        } else {
            sql = "SELECT id, codigo_barras, nome, preco_venda, estoque_atual FROM Produtos WHERE (codigo_barras = '" + busca + "' OR id = '" + busca + "') AND ativo = 1";
        }
        
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                int id = rs.getInt("id");
                String codigo = rs.getString("codigo_barras") != null ? rs.getString("codigo_barras") : String.valueOf(id);
                String nome = rs.getString("nome");
                double preco = rs.getDouble("preco_venda");
                int estoqueAtual = rs.getInt("estoque_atual");

                if (qtdSolicitada > estoqueAtual) {
                    mostrarAviso("Bloqueado: Você tem apenas " + estoqueAtual + " UN disponíveis.");
                    return;
                }

                listaItens.add(new ItemVenda(id, codigo, nome, qtdSolicitada, preco));
                atualizarTotal();
                
                txtBuscaProduto.clear();
                txtQuantidade.setText("1");
                produtoSelecionadoAutocompletar = -1; 
                listaSugestoes.setVisible(false);
                txtBuscaProduto.requestFocus();
                mostrarSucesso("Item adicionado.");
            } else {
                mostrarAviso("Produto não encontrado.");
            }
        } catch (Exception e) { 
            mostrarAviso("Erro ao adicionar: " + e.getMessage()); 
        }
    }

    @FXML
    public void removerItem(ActionEvent event) {
        ItemVenda selecionado = tabelaCarrinho.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            listaItens.remove(selecionado);
            atualizarTotal();
            txtBuscaProduto.requestFocus();
            lblStatus.setText("");
        } else {
            mostrarAviso("Selecione um item para remover.");
        }
    }

    @FXML
    public void alterarQuantidadeItem(ActionEvent event) {
        ItemVenda selecionado = tabelaCarrinho.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarAviso("Selecione um item para alterar a quantidade.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(String.valueOf(selecionado.getQuantidade()));
        dialog.setTitle("Alterar Quantidade");
        dialog.setHeaderText("Modificando: " + selecionado.getNome());
        dialog.setContentText("Nova quantidade:");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/view/estilo.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("dialog-pane");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(novaQtd -> {
            try {
                int qtdNova = Integer.parseInt(novaQtd);
                if (qtdNova > 0) {
                    try (Connection conn = ConexaoSQLite.conectar();
                         PreparedStatement stmt = conn.prepareStatement("SELECT estoque_atual FROM Produtos WHERE id = ?")) {
                        
                        stmt.setInt(1, selecionado.getProdutoId());
                        ResultSet rs = stmt.executeQuery();
                        
                        if (rs.next() && qtdNova > rs.getInt("estoque_atual")) {
                            mostrarAviso("Bloqueado: Estoque insuficiente (" + rs.getInt("estoque_atual") + " UN disponíveis).");
                            return;
                        }
                    }
                    
                    int index = listaItens.indexOf(selecionado);
                    listaItens.set(index, new ItemVenda(selecionado.getProdutoId(), selecionado.getCodigo(), selecionado.getNome(), qtdNova, selecionado.getPrecoUnitario()));
                    
                    atualizarTotal();
                    lblStatus.setText("");
                    txtBuscaProduto.requestFocus();
                } else {
                    mostrarAviso("Quantidade deve ser maior que zero.");
                }
            } catch (Exception e) { 
                mostrarAviso("Valor inválido."); 
            }
        });
    }

    private void atualizarTotal() {
        subtotalVenda = listaItens.stream().mapToDouble(ItemVenda::getSubtotal).sum();
        
        double descontoAplicado = 0.0;
        try {
            if (!txtDesconto.getText().isEmpty()) {
                descontoAplicado = Double.parseDouble(txtDesconto.getText().replace(",", "."));
            }
        } catch (NumberFormatException e) { }

        double percentualJuros = 0.0;
        if ("Fiado (Caderninho)".equals(cbFormaPagamento.getValue())) {
            try {
                if (txtJurosFiado != null && !txtJurosFiado.getText().isEmpty()) {
                    percentualJuros = Double.parseDouble(txtJurosFiado.getText().replace(",", "."));
                }
            } catch (Exception e) {}
        }
        
        double subComDesconto = subtotalVenda - descontoAplicado;
        double valorJurosReal = subComDesconto * (percentualJuros / 100.0);

        totalVenda = subComDesconto + valorJurosReal;
        
        if (totalVenda < 0) totalVenda = 0.0;

        lblSubtotal.setText(String.format("R$ %.2f", subtotalVenda));
        lblTotal.setText(String.format("R$ %.2f", totalVenda));
    }

    @FXML
    public void finalizarVenda(ActionEvent event) {
        // TRAVA DE SEGURANÇA: Impede finalizar se o caixa estiver fechado
        int turnoId = obterTurnoAbertoId();
        if (turnoId == -1) {
            mostrarAviso("ATENÇÃO: Caixa Fechado! Impossível concluir a venda sem abrir o turno.");
            return;
        }

        if (listaItens.isEmpty()) {
            mostrarAviso("O carrinho está vazio.");
            return;
        }

        String formaPagamento = cbFormaPagamento.getValue();
        boolean isFiado = "Fiado (Caderninho)".equals(formaPagamento);
        LocalDate vencimento = dpVencimentoFiado.getValue();

        if (isFiado) {
            if (clienteSelecionadoFiado == -1) {
                mostrarAviso("Acesso Negado: Selecione um cliente da lista para vender Fiado.");
                return;
            }
            if (vencimento == null) {
                mostrarAviso("A data de vencimento é obrigatória para o Fiado.");
                return;
            }
        }
        
        double descontoFinal = 0.0;
        try {
            if (!txtDesconto.getText().isEmpty()) {
                descontoFinal = Double.parseDouble(txtDesconto.getText().replace(",", "."));
            }
        } catch (Exception e) {}
        
        double percentualJuros = 0.0;
        if (isFiado && txtJurosFiado != null) {
            try {
                if (!txtJurosFiado.getText().isEmpty()) {
                    percentualJuros = Double.parseDouble(txtJurosFiado.getText().replace(",", "."));
                }
            } catch (Exception e) {}
        }
        
        double valorJuros = (subtotalVenda - descontoFinal) * (percentualJuros / 100.0);

        try (Connection conn = ConexaoSQLite.conectar()) {
            
            if (isFiado) {
                double limiteCredito = 0;
                double creditoUsado = 0;
                String sqlCheckLimit = "SELECT limite_credito, credito_usado FROM Clientes WHERE id = ?";
                
                try (PreparedStatement stmtCheck = conn.prepareStatement(sqlCheckLimit)) {
                    stmtCheck.setInt(1, clienteSelecionadoFiado);
                    ResultSet rsLimit = stmtCheck.executeQuery();
                    if (rsLimit.next()) {
                        limiteCredito = rsLimit.getDouble("limite_credito");
                        creditoUsado = rsLimit.getDouble("credito_usado");
                    }
                }
                
                double saldoDisponivel = limiteCredito - creditoUsado;
                
                if (totalVenda > saldoDisponivel) {
                    mostrarAviso(String.format("Venda Bloqueada: O cliente tem apenas R$ %.2f de crédito disponível.", saldoDisponivel));
                    return; 
                }
            }

            conn.setAutoCommit(false); 
            try {
                // INSERE O TURNO_ID NA VENDA
                String sqlVenda = "INSERT INTO Vendas (subtotal, desconto, juros, total, forma_pagamento, usuario_id, turno_id) VALUES (?, ?, ?, ?, ?, 1, ?)";
                int vendaId = 0;
                
                try (PreparedStatement stmtVenda = conn.prepareStatement(sqlVenda, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    stmtVenda.setDouble(1, subtotalVenda);
                    stmtVenda.setDouble(2, descontoFinal);
                    stmtVenda.setDouble(3, valorJuros);
                    stmtVenda.setDouble(4, totalVenda);
                    stmtVenda.setString(5, formaPagamento);
                    stmtVenda.setInt(6, turnoId); // ID do Turno Ativo
                    stmtVenda.executeUpdate();
                    
                    ResultSet rsKeys = stmtVenda.getGeneratedKeys();
                    if (rsKeys.next()) {
                        vendaId = rsKeys.getInt(1);
                    }
                }

                String sqlItem = "INSERT INTO Itens_Venda (venda_id, produto_id, quantidade, preco_unitario, subtotal) VALUES (?, ?, ?, ?, ?)";
                String sqlEstoque = "UPDATE Produtos SET estoque_atual = estoque_atual - ? WHERE id = ?";
                String sqlKardex = "INSERT INTO Movimentacao_Estoque (produto_id, tipo, quantidade, motivo) VALUES (?, 'SAIDA', ?, ?)";

                try (PreparedStatement stmtItem = conn.prepareStatement(sqlItem);
                     PreparedStatement stmtEstoque = conn.prepareStatement(sqlEstoque);
                     PreparedStatement stmtKardex = conn.prepareStatement(sqlKardex)) {
                    
                    for (ItemVenda item : listaItens) {
                        stmtItem.setInt(1, vendaId);
                        stmtItem.setInt(2, item.getProdutoId());
                        stmtItem.setInt(3, item.getQuantidade());
                        stmtItem.setDouble(4, item.getPrecoUnitario());
                        stmtItem.setDouble(5, item.getSubtotal());
                        stmtItem.addBatch();

                        stmtEstoque.setInt(1, item.getQuantidade());
                        stmtEstoque.setInt(2, item.getProdutoId());
                        stmtEstoque.addBatch();

                        stmtKardex.setInt(1, item.getProdutoId());
                        stmtKardex.setInt(2, item.getQuantidade());
                        stmtKardex.setString(3, "Venda #" + vendaId);
                        stmtKardex.addBatch();
                    }
                    stmtItem.executeBatch();
                    stmtEstoque.executeBatch();
                    stmtKardex.executeBatch();
                }

                // INSERE O TURNO_ID NO FINANCEIRO TAMBÉM
                String sqlFin = "INSERT INTO Financeiro (tipo, categoria, descricao, valor, data_vencimento, status, cliente_id, turno_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmtFin = conn.prepareStatement(sqlFin)) {
                    if (isFiado) {
                        stmtFin.setString(1, "ENTRADA");
                        stmtFin.setString(2, "Fiado / Caderninho");
                        stmtFin.setString(3, "Fiado: " + txtBuscaCliente.getText().trim() + " (Venda #" + vendaId + ")");
                        stmtFin.setDouble(4, totalVenda);
                        stmtFin.setString(5, vencimento.toString());
                        stmtFin.setString(6, "PENDENTE");
                        stmtFin.setInt(7, clienteSelecionadoFiado);
                        stmtFin.setInt(8, turnoId);
                    } else {
                        stmtFin.setString(1, "ENTRADA");
                        stmtFin.setString(2, "Venda PDV");
                        stmtFin.setString(3, "Venda #" + vendaId + " (" + formaPagamento + ")");
                        stmtFin.setDouble(4, totalVenda);
                        stmtFin.setString(5, LocalDate.now().toString());
                        stmtFin.setString(6, "PAGO");
                        stmtFin.setNull(7, java.sql.Types.INTEGER);
                        stmtFin.setInt(8, turnoId);
                    }
                    stmtFin.executeUpdate();
                }

                if (isFiado) {
                    String sqlUpdateCredito = "UPDATE Clientes SET credito_usado = credito_usado + ? WHERE id = ?";
                    try (PreparedStatement stmtCredito = conn.prepareStatement(sqlUpdateCredito)) {
                        stmtCredito.setDouble(1, totalVenda);
                        stmtCredito.setInt(2, clienteSelecionadoFiado);
                        stmtCredito.executeUpdate();
                    }
                }

                conn.commit(); 
                mostrarSucesso("Venda #" + vendaId + " registrada com sucesso!");
                
                String nomeCliente = isFiado ? txtBuscaCliente.getText().trim() : "";
                gerarCupom80mm(vendaId, descontoFinal, valorJuros, formaPagamento, nomeCliente);
                
                cancelarVenda(null);

            } catch (Exception e) {
                conn.rollback(); 
                mostrarAviso("Erro ao salvar: " + e.getMessage());
            } finally { 
                conn.setAutoCommit(true); 
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }
    
    // ==========================================
    // GERADOR DE CUPOM TÉRMICO (80mm) - WHITE-LABEL E NOME DO ARQUIVO PERSONALIZADO
    // ==========================================
    private void gerarCupom80mm(int vendaId, double descontoAplicado, double jurosAplicado, String formaPag, String cliente) {
        try {
            String pastaRaiz = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TechStoreERP_Relatorios" + File.separator + "Cupons";
            File dir = new File(pastaRaiz);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Busca Nome da Empresa e CNPJ/CPF no banco (White-Label)
            String nomeEmpresa = "SISTEMA DE VENDAS";
            String docEmpresa = "";
            try (Connection conn = ConexaoSQLite.conectar();
                 PreparedStatement stmt = conn.prepareStatement("SELECT nome_empresa, documento_empresa FROM Configuracoes WHERE id = 1");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getString("nome_empresa") != null && !rs.getString("nome_empresa").trim().isEmpty()) {
                        nomeEmpresa = rs.getString("nome_empresa").trim();
                    }
                    if (rs.getString("documento_empresa") != null && !rs.getString("documento_empresa").trim().isEmpty()) {
                        docEmpresa = rs.getString("documento_empresa").trim();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            LocalDateTime agora = LocalDateTime.now();
            String dataHoraCupom = agora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String dataHoraArquivo = agora.format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HHmm"));

            // Monta o nome do arquivo com a Data e o Nome do Cliente (se houver Fiado)
            String sufixoCliente = "";
            if (cliente != null && !cliente.trim().isEmpty()) {
                String clienteLimpo = cliente.trim().replaceAll("[^a-zA-Z0-9À-ÿ _-]", "").replaceAll("\\s+", "_");
                sufixoCliente = "_Fiado_" + clienteLimpo;
            }
            
            String caminhoPdf = pastaRaiz + File.separator + "Cupom_Venda_" + vendaId + "_" + dataHoraArquivo + sufixoCliente + ".pdf";
            
            Rectangle pageSize = new Rectangle(226f, 800f); 
            Document document = new Document(pageSize, 10f, 10f, 10f, 10f);
            PdfWriter.getInstance(document, new FileOutputStream(caminhoPdf));
            
            document.open();
            
            Font fTitulo = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font fNormal = new Font(Font.HELVETICA, 9, Font.NORMAL);
            Font fNegrito = new Font(Font.HELVETICA, 10, Font.BOLD);
            
            StringBuilder textoCabecalho = new StringBuilder();
            textoCabecalho.append(nomeEmpresa.toUpperCase()).append("\n");
            if (!docEmpresa.isEmpty()) {
                textoCabecalho.append("CNPJ/CPF: ").append(docEmpresa).append("\n");
            }
            textoCabecalho.append("Recibo Não-Fiscal\n-----------------------------------------");

            Paragraph cabecalho = new Paragraph(textoCabecalho.toString(), fTitulo);
            cabecalho.setAlignment(Element.ALIGN_CENTER);
            document.add(cabecalho);
            
            document.add(new Paragraph("Venda #" + vendaId + " - " + dataHoraCupom + "\n ", fNormal));
            
            if (cliente != null && !cliente.isEmpty()) {
                document.add(new Paragraph("Cliente: " + cliente + "\n ", fNormal));
            }
            
            PdfPTable table = new PdfPTable(3); 
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 5f, 2.5f});
            
            for (ItemVenda item : listaItens) {
                table.addCell(new PdfPCell(new Paragraph(item.getQuantidade() + "x", fNormal)));
                table.addCell(new PdfPCell(new Paragraph(item.getNome(), fNormal)));
                table.addCell(new PdfPCell(new Paragraph(String.format("R$ %.2f", item.getSubtotal()), fNormal)));
            }
            
            for (PdfPRow row : table.getRows()) {
                for (PdfPCell cell : row.getCells()) {
                    if (cell != null) cell.setBorder(Rectangle.NO_BORDER);
                }
            }
            document.add(table);
            
            document.add(new Paragraph("\n-----------------------------------------", fNormal));
            document.add(new Paragraph("Subtotal: R$ " + String.format("%.2f", subtotalVenda), fNormal));
            
            if (descontoAplicado > 0) {
                document.add(new Paragraph("Desconto: - R$ " + String.format("%.2f", descontoAplicado), fNormal));
            }
            if (jurosAplicado > 0) {
                document.add(new Paragraph("Juros Aplicado: + R$ " + String.format("%.2f", jurosAplicado), fNormal));
            }
            
            document.add(new Paragraph("TOTAL: R$ " + String.format("%.2f", totalVenda), fNegrito));
            document.add(new Paragraph("Pagamento: " + formaPag, fNormal));
            document.add(new Paragraph("\n\nObrigado pela preferência!\nVolte Sempre.\n", fNormal));
            
            document.close();
            
            abrirNoSistemaOperacional(caminhoPdf); 
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void abrirNoSistemaOperacional(String caminho) {
        File arquivo = new File(caminho);
        if (!arquivo.exists()) return;
        
        new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    new ProcessBuilder("cmd", "/c", "start", "", arquivo.getAbsolutePath()).start();
                } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", arquivo.getAbsolutePath()});
                } else {
                    java.awt.Desktop.getDesktop().open(arquivo);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    @FXML
    public void cancelarVenda(ActionEvent event) {
        listaItens.clear();
        txtDesconto.setText("0.00");
        
        if (txtJurosFiado != null) {
            txtJurosFiado.setText("");
        }
        
        atualizarTotal();
        txtBuscaProduto.clear();
        txtQuantidade.setText("1");
        
        txtBuscaCliente.clear();
        clienteSelecionadoFiado = -1;
        listaSugestoesCliente.setVisible(false);
        
        if(event != null) lblStatus.setText("");
        txtBuscaProduto.requestFocus();
    }
    
    private void mostrarSucesso(String msg) {
        lblStatus.setStyle("-fx-text-fill: #2ed573;");
        lblStatus.setText(msg);
    }

    private void mostrarAviso(String msg) {
        lblStatus.setStyle("-fx-text-fill: #ffce00;");
        lblStatus.setText(msg);
    }
}