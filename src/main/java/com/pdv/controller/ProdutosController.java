package com.pdv.controller;

import com.pdv.dao.ConexaoSQLite;
import com.pdv.model.Produto;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ProdutosController {

    @FXML private TextField txtBusca;
    @FXML private TextField txtCodigoBarras;
    @FXML private TextField txtNome;
    @FXML private ComboBox<String> cbUnidade;
    @FXML private TextField txtNcm;
    
    // Nível 2
    @FXML private TextField txtCategoria;
    @FXML private TextField txtFornecedor;
    
    @FXML private TextField txtPrecoCusto;
    @FXML private TextField txtPrecoVenda;
    @FXML private TextField txtEstoqueInicial;
    @FXML private TextField txtEstoqueMinimo;
    @FXML private Label lblStatus;

    @FXML private TableView<Produto> tabelaProdutos;
    @FXML private TableColumn<Produto, String> colCodigo;
    @FXML private TableColumn<Produto, String> colNome;
    @FXML private TableColumn<Produto, String> colUnidade;
    @FXML private TableColumn<Produto, String> colCusto;
    @FXML private TableColumn<Produto, String> colVenda;
    @FXML private TableColumn<Produto, Number> colEstoque;
    @FXML private TableColumn<Produto, Number> colMinimo;
    
    // Nível 2
    @FXML private TableColumn<Produto, String> colCategoria;
    @FXML private TableColumn<Produto, String> colFornecedor;

    @FXML private ComboBox<String> cbTipoMovimento;
    @FXML private TextField txtQtdMovimento;
    @FXML private TextField txtMotivoMovimento;

    private Integer produtoSelecionadoId = null;
    private ObservableList<Produto> listaProdutos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cbUnidade.setItems(FXCollections.observableArrayList("UN", "KG", "LT", "CX", "PC"));
        cbUnidade.setValue("UN");

        cbTipoMovimento.setItems(FXCollections.observableArrayList("ENTRADA", "SAIDA"));
        cbTipoMovimento.setValue("ENTRADA");

        colCodigo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCodigoBarras()));
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNome()));
        colUnidade.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnidade()));
        colCusto.setCellValueFactory(data -> new SimpleStringProperty(String.format("R$ %.2f", data.getValue().getPrecoCusto())));
        colVenda.setCellValueFactory(data -> new SimpleStringProperty(String.format("R$ %.2f", data.getValue().getPrecoVenda())));
        colEstoque.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getEstoqueAtual()));
        colMinimo.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getEstoqueMinimo()));
        
        // Nível 2
        colCategoria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoria()));
        colFornecedor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFornecedor()));
        
        // Alerta Visual de Estoque Baixo na Tabela
        colEstoque.setCellFactory(column -> new TableCell<Produto, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    Produto p = getTableView().getItems().get(getIndex());
                    if (p.getEstoqueAtual() <= p.getEstoqueMinimo()) {
                        setStyle("-fx-text-fill: #ff5252; -fx-font-weight: bold;"); 
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        tabelaProdutos.getSelectionModel().selectedItemProperty().addListener((obs, antigo, selecionado) -> {
            if (selecionado != null) preencherFormulario(selecionado);
        });

        carregarProdutos("");
    }

    private void preencherFormulario(Produto p) {
        produtoSelecionadoId = p.getId();
        txtCodigoBarras.setText(p.getCodigoBarras());
        txtNome.setText(p.getNome());
        cbUnidade.setValue(p.getUnidade());
        txtNcm.setText(p.getNcm());
        txtPrecoCusto.setText(String.format("%.2f", p.getPrecoCusto()).replace(",", "."));
        txtPrecoVenda.setText(String.format("%.2f", p.getPrecoVenda()).replace(",", "."));
        txtEstoqueInicial.setText(String.valueOf(p.getEstoqueAtual()));
        txtEstoqueInicial.setDisable(true); 
        txtEstoqueMinimo.setText(String.valueOf(p.getEstoqueMinimo()));
        
        // Nível 2
        txtCategoria.setText(p.getCategoria());
        txtFornecedor.setText(p.getFornecedor());
        
        mostrarMensagem("Editando produto ID #" + p.getId(), "#2196F3");
    }

    @FXML
    public void limparFormulario(ActionEvent event) {
        produtoSelecionadoId = null;
        txtCodigoBarras.clear();
        txtNome.clear();
        cbUnidade.setValue("UN");
        txtNcm.setText("00000000");
        txtPrecoCusto.clear();
        txtPrecoVenda.clear();
        txtEstoqueInicial.clear();
        txtEstoqueInicial.setDisable(false);
        txtEstoqueMinimo.setText("5");
        
        txtCategoria.clear();
        txtFornecedor.clear();
        
        tabelaProdutos.getSelectionModel().clearSelection();
        mostrarMensagem("Formulário pronto para novo cadastro.", "#8e8e9e");
    }

    @FXML
    public void salvarProduto(ActionEvent event) {
        String nome = txtNome.getText().trim();
        String codigo = txtCodigoBarras.getText().trim();
        String unidade = cbUnidade.getValue();
        String ncm = txtNcm.getText().trim().isEmpty() ? "00000000" : txtNcm.getText().trim();
        
        // Nível 2: Se vier vazio, coloca um padrão
        String categoria = txtCategoria.getText().trim().isEmpty() ? "Geral" : txtCategoria.getText().trim();
        String fornecedor = txtFornecedor.getText().trim().isEmpty() ? "Não Informado" : txtFornecedor.getText().trim();

        if (nome.isEmpty() || txtPrecoVenda.getText().trim().isEmpty()) {
            mostrarMensagem("Preencha pelo menos o Nome e o Preço de Venda!", "#ffce00");
            return;
        }

        try {
            double custo = txtPrecoCusto.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtPrecoCusto.getText().replace(",", "."));
            double venda = Double.parseDouble(txtPrecoVenda.getText().replace(",", "."));
            int estoque = txtEstoqueInicial.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtEstoqueInicial.getText().trim());
            int minimo = txtEstoqueMinimo.getText().trim().isEmpty() ? 5 : Integer.parseInt(txtEstoqueMinimo.getText().trim());

            try (Connection conn = ConexaoSQLite.conectar()) {
                if (produtoSelecionadoId == null) {
                    String sql = "INSERT INTO Produtos (codigo_barras, nome, unidade, ncm, preco_custo, preco_venda, estoque_atual, estoque_minimo, categoria, fornecedor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, codigo.isEmpty() ? null : codigo);
                        stmt.setString(2, nome);
                        stmt.setString(3, unidade);
                        stmt.setString(4, ncm);
                        stmt.setDouble(5, custo);
                        stmt.setDouble(6, venda);
                        stmt.setInt(7, estoque);
                        stmt.setInt(8, minimo);
                        stmt.setString(9, categoria);
                        stmt.setString(10, fornecedor);
                        stmt.executeUpdate();
                    }
                    mostrarMensagem("Produto cadastrado com sucesso!", "#4CAF50");
                } else {
                    String sql = "UPDATE Produtos SET codigo_barras=?, nome=?, unidade=?, ncm=?, preco_custo=?, preco_venda=?, estoque_minimo=?, categoria=?, fornecedor=? WHERE id=?";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, codigo.isEmpty() ? null : codigo);
                        stmt.setString(2, nome);
                        stmt.setString(3, unidade);
                        stmt.setString(4, ncm);
                        stmt.setDouble(5, custo);
                        stmt.setDouble(6, venda);
                        stmt.setInt(7, minimo);
                        stmt.setString(8, categoria);
                        stmt.setString(9, fornecedor);
                        stmt.setInt(10, produtoSelecionadoId);
                        stmt.executeUpdate();
                    }
                    mostrarMensagem("Produto atualizado com sucesso!", "#4CAF50");
                }
            }

            limparFormulario(null);
            carregarProdutos(txtBusca.getText());

        } catch (NumberFormatException e) {
            mostrarMensagem("Erro: Digite valores numéricos válidos para preço e estoque.", "#ff5252");
        } catch (Exception e) {
            mostrarMensagem("Erro ao salvar (verifique se o código já existe): " + e.getMessage(), "#ff5252");
        }
    }

    @FXML
    public void registrarMovimentacao(ActionEvent event) {
        if (produtoSelecionadoId == null) {
            mostrarMensagem("Selecione um produto na tabela primeiro para movimentar o estoque!", "#ffce00");
            return;
        }

        try {
            int qtd = Integer.parseInt(txtQtdMovimento.getText().trim());
            if (qtd <= 0) {
                mostrarMensagem("A quantidade deve ser maior que zero.", "#ffce00");
                return;
            }

            String tipo = cbTipoMovimento.getValue();
            String motivo = txtMotivoMovimento.getText().trim().isEmpty() ? "Ajuste Manual" : txtMotivoMovimento.getText().trim();
            int fator = tipo.equals("ENTRADA") ? qtd : -qtd;

            try (Connection conn = ConexaoSQLite.conectar()) {
                String sqlUpdate = "UPDATE Produtos SET estoque_atual = estoque_atual + ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    stmt.setInt(1, fator);
                    stmt.setInt(2, produtoSelecionadoId);
                    stmt.executeUpdate();
                }

                String sqlHist = "INSERT INTO Movimentacao_Estoque (produto_id, tipo, quantidade, motivo) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sqlHist)) {
                    stmt.setInt(1, produtoSelecionadoId);
                    stmt.setString(2, tipo);
                    stmt.setInt(3, qtd);
                    stmt.setString(4, motivo);
                    stmt.executeUpdate();
                }
            }

            txtQtdMovimento.clear();
            txtMotivoMovimento.clear();
            carregarProdutos(txtBusca.getText());
            mostrarMensagem("Movimentação (" + tipo + " de " + qtd + ") registrada!", "#4CAF50");

        } catch (NumberFormatException e) {
            mostrarMensagem("Informe uma quantidade inteira válida para movimentar.", "#ff5252");
        } catch (Exception e) {
            mostrarMensagem("Erro ao movimentar estoque: " + e.getMessage(), "#ff5252");
        }
    }

    @FXML
    public void excluirProduto(ActionEvent event) {
        if (produtoSelecionadoId == null) {
            mostrarMensagem("Selecione um produto na tabela para excluir.", "#ffce00");
            return;
        }

        String sql = "UPDATE Produtos SET ativo = 0 WHERE id = ?";
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, produtoSelecionadoId);
            stmt.executeUpdate();
            limparFormulario(null);
            carregarProdutos(txtBusca.getText());
            mostrarMensagem("Produto removido da listagem ativa.", "#4CAF50");
        } catch (Exception e) {
            mostrarMensagem("Erro ao remover: " + e.getMessage(), "#ff5252");
        }
    }

    @FXML
    public void pesquisarProdutos(KeyEvent event) {
        carregarProdutos(txtBusca.getText());
    }

    private void carregarProdutos(String filtro) {
        listaProdutos.clear();
        String sql = "SELECT * FROM Produtos WHERE ativo = 1 AND (nome LIKE ? OR codigo_barras LIKE ?) ORDER BY nome ASC";

        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String busca = "%" + (filtro == null ? "" : filtro.trim()) + "%";
            stmt.setString(1, busca);
            stmt.setString(2, busca);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                listaProdutos.add(new Produto(
                        rs.getInt("id"),
                        rs.getString("codigo_barras") != null ? rs.getString("codigo_barras") : "",
                        rs.getString("nome"),
                        rs.getString("unidade"),
                        rs.getString("ncm"),
                        rs.getDouble("preco_custo"),
                        rs.getDouble("preco_venda"),
                        rs.getInt("estoque_atual"),
                        rs.getInt("estoque_minimo"),
                        rs.getString("categoria"),
                        rs.getString("fornecedor")
                ));
            }
            tabelaProdutos.setItems(listaProdutos);
        } catch (Exception e) {
            mostrarMensagem("Erro ao carregar lista: " + e.getMessage(), "#ff5252");
        }
    }

    private void mostrarMensagem(String texto, String corHex) {
        lblStatus.setStyle("-fx-text-fill: " + corHex + "; -fx-font-weight: bold;");
        lblStatus.setText(texto);
    }
}