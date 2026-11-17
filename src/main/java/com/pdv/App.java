package com.pdv;

import com.pdv.dao.SetupBanco;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class App extends Application {

    // Guarda o serviço do SO para abrir arquivos/links
    private static HostServices hostServices;

    public static HostServices getHostServicesStatic() {
        return hostServices;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Inicializa o HostServices
        hostServices = getHostServices();

        SetupBanco.criarTabelas();

        Parent root = FXMLLoader.load(getClass().getResource("/view/Login.fxml"));
        primaryStage.setTitle("Sistema de Vendas e Controle de estoque - Acesso Restrito");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.initStyle(StageStyle.UNDECORATED); 
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}