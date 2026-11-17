package com.pdv.controller;

import com.pdv.dao.ConexaoSQLite;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtSenha;
    @FXML private TextField txtSenhaVisivel;
    @FXML private Button btnMostrarSenha;
    @FXML private Label lblMensagem;
    @FXML private Label lblNomeEmpresa;

    private boolean senhaVisivel = false;

    @FXML
    public void initialize() {
        txtSenhaVisivel.textProperty().bindBidirectional(txtSenha.textProperty());
        
        // WHITE-LABEL: Lê o nome da empresa do banco para exibir na tela de Login
        Platform.runLater(() -> {
            try (Connection conn = ConexaoSQLite.conectar();
                 PreparedStatement stmt = conn.prepareStatement("SELECT nome_empresa FROM Configuracoes WHERE id = 1");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String nome = rs.getString("nome_empresa");
                    lblNomeEmpresa.setText(nome);
                }
            } catch (Exception e) {}
        });
    }

    @FXML
    public void alternarVisibilidadeSenha(ActionEvent event) {
        senhaVisivel = !senhaVisivel;

        if (senhaVisivel) {
            txtSenhaVisivel.setVisible(true);
            txtSenhaVisivel.setManaged(true);
            txtSenha.setVisible(false);
            txtSenha.setManaged(false);
            btnMostrarSenha.setText("Ocultar");
            txtSenhaVisivel.requestFocus();
            txtSenhaVisivel.positionCaret(txtSenhaVisivel.getText().length());
        } else {
            txtSenha.setVisible(true);
            txtSenha.setManaged(true);
            txtSenhaVisivel.setVisible(false);
            txtSenhaVisivel.setManaged(false);
            btnMostrarSenha.setText("Ver");
            txtSenha.requestFocus();
            txtSenha.positionCaret(txtSenha.getText().length());
        }
    }

    @FXML
    public void fazerLogin(ActionEvent event) {
        String user = txtUsuario.getText().trim();
        String pass = txtSenha.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            lblMensagem.setStyle("-fx-text-fill: #ffce00;");
            lblMensagem.setText("Preencha todos os campos.");
            return;
        }

        String sql = "SELECT nivel_acesso FROM Usuarios WHERE nome = ? AND senha = ? AND ativo = 1";

        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user);
            stmt.setString(2, pass);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int nivel = rs.getInt("nivel_acesso");

                URL urlDashboard = getClass().getResource("/view/Dashboard.fxml");
                if (urlDashboard == null) {
                    lblMensagem.setStyle("-fx-text-fill: #ff5252;");
                    lblMensagem.setText("Erro interno: Arquivo Dashboard.fxml não encontrado!");
                    return;
                }

                // Fecha a janela de login antes de abrir a principal para liberar o foco do gerenciador de janelas
                Stage stageAtual = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stageAtual.close();

                Rectangle2D limitesTela = Screen.getPrimary().getVisualBounds();
                Parent raiz = FXMLLoader.load(urlDashboard);
                
                Stage stageDashboard = new Stage();
                stageDashboard.setTitle(lblNomeEmpresa.getText() + " - Sistema de Gestão (Nível " + nivel + ")");
                stageDashboard.setResizable(true);
                
                Scene cenaDashboard = new Scene(raiz, limitesTela.getWidth(), limitesTela.getHeight());
                stageDashboard.setScene(cenaDashboard);
                
                // Exibe a janela e aplica as dimensões máximas imediatamente após o mapeamento no Linux/Windows
                stageDashboard.show();
                stageDashboard.setX(limitesTela.getMinX());
                stageDashboard.setY(limitesTela.getMinY());
                stageDashboard.setWidth(limitesTela.getWidth());
                stageDashboard.setHeight(limitesTela.getHeight());
                stageDashboard.setMaximized(true);

                Platform.runLater(() -> {
                    stageDashboard.setX(limitesTela.getMinX());
                    stageDashboard.setY(limitesTela.getMinY());
                    stageDashboard.setWidth(limitesTela.getWidth());
                    stageDashboard.setHeight(limitesTela.getHeight());
                    stageDashboard.setMaximized(false);
                    stageDashboard.setMaximized(true);
                });

            } else {
                lblMensagem.setStyle("-fx-text-fill: #ff5252;");
                lblMensagem.setText("Usuário ou senha incorretos.");
            }
        } catch (Exception e) {
            e.printStackTrace(); 
            lblMensagem.setStyle("-fx-text-fill: #ff5252;");
            lblMensagem.setText("Erro ao abrir tela: " + e.getMessage());
        }
    }
}