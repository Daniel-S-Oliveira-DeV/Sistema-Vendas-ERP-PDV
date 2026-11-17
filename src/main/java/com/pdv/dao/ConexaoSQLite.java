package com.pdv.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoSQLite {
    // O banco será criado na raiz do projeto com este nome
    private static final String URL = "jdbc:sqlite:banco_pdv.db";

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("Erro ao conectar no banco de dados: " + e.getMessage());
            return null;
        }
    }
}