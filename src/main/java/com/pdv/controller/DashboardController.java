package com.pdv.controller;

import com.pdv.dao.ConexaoSQLite;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class DashboardController {

    @FXML private StackPane areaCentral;
    @FXML private Label lblNomeEmpresa;
    
    // VARIÁVEIS DO ALERTA FIXO (Nível 3)
    @FXML private VBox boxAlertaFiados;
    @FXML private Label lblAlertaFiados;

    @FXML
    public void initialize() {
        // Assim que o painel abrir, checa os fiados atrasados no banco
        Platform.runLater(this::verificarFiadosAtrasados);
        
        // NÍVEL 3 (WHITE-LABEL): Lê o nome da empresa e exibe no menu lateral
        Platform.runLater(this::carregarNomeEmpresa);
    }

    private void carregarNomeEmpresa() {
        String sql = "SELECT nome_empresa FROM Configuracoes WHERE id = 1";
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                lblNomeEmpresa.setText(rs.getString("nome_empresa"));
            }
        } catch (Exception e) {
            System.out.println("Erro ao carregar nome da empresa: " + e.getMessage());
        }
    }

    private void verificarFiadosAtrasados() {
        String sql = "SELECT descricao, valor, data_vencimento FROM Financeiro " +
                     "WHERE categoria = 'Fiado / Caderninho' AND status = 'PENDENTE' AND data_vencimento < ?";
        
        StringBuilder devedores = new StringBuilder();
        int contasAtrasadas = 0;

        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Passa a data de HOJE. Tudo que for menor que hoje está atrasado.
            stmt.setString(1, LocalDate.now().toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                contasAtrasadas++;
                devedores.append(rs.getString("descricao"))
                         .append(" - R$ ").append(String.format("%.2f", rs.getDouble("valor")))
                         .append(" (Venceu em: ").append(rs.getString("data_vencimento")).append(")\n");
            }

            // Se encontrou devedores, exibe um Pop-up (Alert) na tela!
            if (contasAtrasadas > 0) {
                Alert alerta = new Alert(Alert.AlertType.WARNING);
                alerta.setTitle("Aviso de Inadimplência");
                alerta.setHeaderText("Atenção: Existem " + contasAtrasadas + " contas de Fiado em atraso!");
                alerta.setContentText(devedores.toString());
                alerta.showAndWait();
                
                // Exibe o painel fixo vermelho na tela principal (Nível 3)
                if (boxAlertaFiados != null) {
                    boxAlertaFiados.setVisible(true);
                    boxAlertaFiados.setManaged(true);
                    lblAlertaFiados.setText("Lista de Devedores:\n" + devedores.toString());
                }
            } else {
                if (boxAlertaFiados != null) {
                    boxAlertaFiados.setVisible(false);
                    boxAlertaFiados.setManaged(false);
                }
            }
            
        } catch (Exception e) {
            System.out.println("Erro ao checar inadimplência: " + e.getMessage());
        }
    }

    private void carregarTela(String caminhoFxml) {
        try {
            Parent tela = FXMLLoader.load(getClass().getResource(caminhoFxml));
            areaCentral.getChildren().setAll(tela);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // NOVO MÉTODO PARA ABRIR O CAIXA
    @FXML
    public void abrirCaixa(ActionEvent event) {
        carregarTela("/view/Caixa.fxml");
    }

    @FXML
    public void abrirPDV(ActionEvent event) {
        carregarTela("/view/PDV.fxml");
    }

    @FXML
    public void abrirProdutos(ActionEvent event) {
        carregarTela("/view/Produtos.fxml");
    }

    @FXML
    public void abrirClientes(ActionEvent event) {
        carregarTela("/view/Clientes.fxml");
    }

    @FXML
    public void abrirFinanceiro(ActionEvent event) {
        carregarTela("/view/Financeiro.fxml");
    }

    @FXML
    public void abrirRelatorios(ActionEvent event) {
        carregarTela("/view/Relatorios.fxml");
    }
    
    @FXML
    public void abrirConfiguracoes(ActionEvent event) {
        // Agora o botão realmente carrega a tela de configurações em vez de apenas imprimir no console
        carregarTela("/view/Configuracoes.fxml");
    }

    @FXML
    public void sairSistema(ActionEvent event) {
        Platform.exit();
    }
}