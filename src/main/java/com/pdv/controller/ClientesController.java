package com.pdv.controller;

import com.pdv.dao.ConexaoSQLite;
import com.pdv.model.Cliente;
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

public class ClientesController {

    @FXML private TextField txtBusca;
    @FXML private TextField txtNome;
    @FXML private TextField txtCpf;
    @FXML private TextField txtTelefone;
    @FXML private TextField txtEndereco;
    
    // Nível 2
    @FXML private TextField txtLimite;
    @FXML private TextField txtCreditoUsado;
    
    @FXML private Label lblStatus;

    @FXML private TableView<Cliente> tabelaClientes;
    @FXML private TableColumn<Cliente, String> colNome;
    @FXML private TableColumn<Cliente, String> colCpf;
    @FXML private TableColumn<Cliente, String> colTelefone;
    @FXML private TableColumn<Cliente, String> colEndereco;
    
    // Nível 2
    @FXML private TableColumn<Cliente, String> colLimite;
    @FXML private TableColumn<Cliente, String> colDisponivel;

    private ObservableList<Cliente> listaClientes = FXCollections.observableArrayList();
    private Cliente clienteSelecionado;

    @FXML
    public void initialize() {
        colNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNome()));
        colCpf.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCpf()));
        colTelefone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTelefone()));
        colEndereco.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEndereco()));
        
        // Formata as colunas financeiras
        colLimite.setCellValueFactory(data -> new SimpleStringProperty(String.format("R$ %.2f", data.getValue().getLimiteCredito())));
        colDisponivel.setCellValueFactory(data -> new SimpleStringProperty(String.format("R$ %.2f", data.getValue().getSaldoDisponivel())));
        
        tabelaClientes.setItems(listaClientes);

        tabelaClientes.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) preencherFormulario(newSel);
        });

        // Impede a digitação de letras no campo de limite
        txtLimite.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtLimite.setText(newVal.replaceAll("[^\\d.]", ""));
            }
        });

        carregarClientes("");
    }

    private void carregarClientes(String busca) {
        listaClientes.clear();
        String sql = "SELECT id, nome, cpf, telefone, endereco, limite_credito, credito_usado FROM Clientes WHERE nome LIKE ? OR cpf LIKE ? ORDER BY nome ASC";
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + busca + "%");
            stmt.setString(2, "%" + busca + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                listaClientes.add(new Cliente(
                        rs.getInt("id"), rs.getString("nome"), rs.getString("cpf"),
                        rs.getString("telefone"), rs.getString("endereco"),
                        rs.getDouble("limite_credito"), rs.getDouble("credito_usado")
                ));
            }
        } catch (Exception e) {
            lblStatus.setText("Erro ao carregar: " + e.getMessage());
        }
    }

    @FXML
    public void pesquisarClientes(KeyEvent event) {
        carregarClientes(txtBusca.getText().trim());
    }

    private void preencherFormulario(Cliente c) {
        clienteSelecionado = c;
        txtNome.setText(c.getNome());
        txtCpf.setText(c.getCpf());
        txtTelefone.setText(c.getTelefone());
        txtEndereco.setText(c.getEndereco());
        txtLimite.setText(String.format("%.2f", c.getLimiteCredito()).replace(",", "."));
        txtCreditoUsado.setText(String.format("%.2f", c.getCreditoUsado()).replace(",", "."));
        lblStatus.setText("");
    }

    @FXML
    public void limparFormulario(ActionEvent event) {
        clienteSelecionado = null;
        txtNome.clear();
        txtCpf.clear();
        txtTelefone.clear();
        txtEndereco.clear();
        txtLimite.clear();
        txtCreditoUsado.clear();
        tabelaClientes.getSelectionModel().clearSelection();
        lblStatus.setText("");
    }

    @FXML
    public void salvarCliente(ActionEvent event) {
        String nome = txtNome.getText().trim();
        if (nome.isEmpty()) {
            lblStatus.setStyle("-fx-text-fill: #ffce00;");
            lblStatus.setText("O nome é obrigatório.");
            return;
        }
        
        double limite = 0.0;
        try {
            if (!txtLimite.getText().isEmpty()) limite = Double.parseDouble(txtLimite.getText().replace(",", "."));
        } catch (Exception e) {}

        try (Connection conn = ConexaoSQLite.conectar()) {
            if (clienteSelecionado == null) {
                String sql = "INSERT INTO Clientes (nome, cpf, telefone, endereco, limite_credito) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, nome);
                    stmt.setString(2, txtCpf.getText().trim());
                    stmt.setString(3, txtTelefone.getText().trim());
                    stmt.setString(4, txtEndereco.getText().trim());
                    stmt.setDouble(5, limite);
                    stmt.executeUpdate();
                    mostrarSucesso("Cliente registrado com sucesso!");
                }
            } else {
                String sql = "UPDATE Clientes SET nome = ?, cpf = ?, telefone = ?, endereco = ?, limite_credito = ? WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, nome);
                    stmt.setString(2, txtCpf.getText().trim());
                    stmt.setString(3, txtTelefone.getText().trim());
                    stmt.setString(4, txtEndereco.getText().trim());
                    stmt.setDouble(5, limite);
                    stmt.setInt(6, clienteSelecionado.getId());
                    stmt.executeUpdate();
                    mostrarSucesso("Cliente atualizado!");
                }
            }
            carregarClientes(txtBusca.getText().trim());
            limparFormulario(null);
        } catch (Exception e) {
            lblStatus.setStyle("-fx-text-fill: #ff5252;");
            lblStatus.setText("Erro ao salvar: " + e.getMessage());
        }
    }

    @FXML
    public void excluirCliente(ActionEvent event) {
        if (clienteSelecionado == null) {
            lblStatus.setStyle("-fx-text-fill: #ffce00;");
            lblStatus.setText("Selecione um cliente para excluir.");
            return;
        }
        String sql = "DELETE FROM Clientes WHERE id = ?";
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clienteSelecionado.getId());
            stmt.executeUpdate();
            mostrarSucesso("Cliente apagado.");
            carregarClientes(txtBusca.getText().trim());
            limparFormulario(null);
        } catch (Exception e) {
            lblStatus.setStyle("-fx-text-fill: #ff5252;");
            lblStatus.setText("Erro ao excluir.");
        }
    }
    
    private void mostrarSucesso(String msg) {
        lblStatus.setStyle("-fx-text-fill: #2ed573;");
        lblStatus.setText(msg);
    }
}