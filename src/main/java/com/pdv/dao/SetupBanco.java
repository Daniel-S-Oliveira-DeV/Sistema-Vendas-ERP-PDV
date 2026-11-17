package com.pdv.dao;

import java.sql.Connection;
import java.sql.Statement;

public class SetupBanco {
    
    public static void criarTabelas() {
        
        // TABELAS ORIGINAIS
        String sqlUsuarios = "CREATE TABLE IF NOT EXISTS Usuarios (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL UNIQUE, senha TEXT NOT NULL, nivel_acesso INTEGER NOT NULL, ativo INTEGER DEFAULT 1);";
        String sqlProdutos = "CREATE TABLE IF NOT EXISTS Produtos (id INTEGER PRIMARY KEY AUTOINCREMENT, codigo_barras TEXT UNIQUE, nome TEXT NOT NULL, unidade TEXT DEFAULT 'UN', ncm TEXT DEFAULT '00000000', preco_custo REAL NOT NULL DEFAULT 0.0, preco_venda REAL NOT NULL, estoque_atual INTEGER NOT NULL DEFAULT 0, estoque_minimo INTEGER DEFAULT 5, ativo INTEGER DEFAULT 1);";
        String sqlMovEstoque = "CREATE TABLE IF NOT EXISTS Movimentacao_Estoque (id INTEGER PRIMARY KEY AUTOINCREMENT, produto_id INTEGER NOT NULL, tipo TEXT NOT NULL, quantidade INTEGER NOT NULL, motivo TEXT, data_movimentacao DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(produto_id) REFERENCES Produtos(id));";
        String sqlVendas = "CREATE TABLE IF NOT EXISTS Vendas (id INTEGER PRIMARY KEY AUTOINCREMENT, data_venda DATETIME DEFAULT CURRENT_TIMESTAMP, subtotal REAL NOT NULL, desconto REAL DEFAULT 0.0, total REAL NOT NULL, forma_pagamento TEXT NOT NULL, usuario_id INTEGER, FOREIGN KEY(usuario_id) REFERENCES Usuarios(id));";
        String sqlItensVenda = "CREATE TABLE IF NOT EXISTS Itens_Venda (id INTEGER PRIMARY KEY AUTOINCREMENT, venda_id INTEGER NOT NULL, produto_id INTEGER NOT NULL, quantidade INTEGER NOT NULL, preco_unitario REAL NOT NULL, subtotal REAL NOT NULL, FOREIGN KEY(venda_id) REFERENCES Vendas(id), FOREIGN KEY(produto_id) REFERENCES Produtos(id));";
        String sqlFinanceiro = "CREATE TABLE IF NOT EXISTS Financeiro (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT NOT NULL, categoria TEXT NOT NULL, descricao TEXT NOT NULL, valor REAL NOT NULL, data_vencimento DATE NOT NULL, data_pagamento DATE, status TEXT NOT NULL DEFAULT 'PAGO');";
        String sqlConfig = "CREATE TABLE IF NOT EXISTS Configuracoes (id INTEGER PRIMARY KEY CHECK (id = 1), nome_empresa TEXT DEFAULT 'Sistema de Vendas e Controle de estoque', documento_empresa TEXT DEFAULT '00.000.000/0000-00', cor_primaria TEXT DEFAULT '#2196F3', caminho_logo TEXT);";
        String sqlHistoricoRelatorios = "CREATE TABLE IF NOT EXISTS Historico_Relatorios (id INTEGER PRIMARY KEY AUTOINCREMENT, tipo TEXT NOT NULL, periodo TEXT NOT NULL, data_geracao TEXT NOT NULL, caminho_arquivo TEXT NOT NULL);";
        String sqlClientes = "CREATE TABLE IF NOT EXISTS Clientes (id INTEGER PRIMARY KEY AUTOINCREMENT, nome TEXT NOT NULL, cpf TEXT, telefone TEXT, endereco TEXT);";
        
        // TABELAS DE CAIXA / TURNO
        String sqlTurnos = "CREATE TABLE IF NOT EXISTS Caixa_Turnos (id INTEGER PRIMARY KEY AUTOINCREMENT, data_abertura DATETIME DEFAULT CURRENT_TIMESTAMP, valor_abertura REAL NOT NULL, data_fechamento DATETIME, valor_fechamento REAL, status TEXT NOT NULL DEFAULT 'ABERTO');";
        String sqlMovimentosCaixa = "CREATE TABLE IF NOT EXISTS Caixa_Movimentos (id INTEGER PRIMARY KEY AUTOINCREMENT, turno_id INTEGER NOT NULL, tipo TEXT NOT NULL, valor REAL NOT NULL, descricao TEXT, data_movimento DATETIME DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY(turno_id) REFERENCES Caixa_Turnos(id));";

        String sqlAdmin = "INSERT OR IGNORE INTO Usuarios (nome, senha, nivel_acesso) VALUES ('admin', 'admin', 1);";
        String sqlConfigInicial = "INSERT OR IGNORE INTO Configuracoes (id, nome_empresa, cor_primaria) VALUES (1, 'Sistema de Vendas e Controle de estoque', '#2196F3');";

        try (Connection conn = ConexaoSQLite.conectar();
             Statement stmt = conn.createStatement()) {
            
            // 1. Cria ou verifica todas as tabelas
            stmt.execute(sqlUsuarios);
            stmt.execute(sqlProdutos);
            stmt.execute(sqlMovEstoque);
            stmt.execute(sqlVendas);
            stmt.execute(sqlItensVenda);
            stmt.execute(sqlFinanceiro);
            stmt.execute(sqlConfig);
            stmt.execute(sqlHistoricoRelatorios);
            stmt.execute(sqlClientes);
            stmt.execute(sqlTurnos);
            stmt.execute(sqlMovimentosCaixa);
            
            // 2. INJEÇÃO DE COLUNAS NOVAS NAS TABELAS EXISTENTES
            try { stmt.execute("ALTER TABLE Vendas ADD COLUMN turno_id INTEGER;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Financeiro ADD COLUMN turno_id INTEGER;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Clientes ADD COLUMN limite_credito REAL DEFAULT 0.0;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Clientes ADD COLUMN credito_usado REAL DEFAULT 0.0;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Financeiro ADD COLUMN valor_pago REAL DEFAULT 0.0;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Financeiro ADD COLUMN cliente_id INTEGER;"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Vendas ADD COLUMN status TEXT DEFAULT 'CONCLUIDA';"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Produtos ADD COLUMN categoria TEXT DEFAULT 'Geral';"); } catch (Exception ignored) {}
            try { stmt.execute("ALTER TABLE Produtos ADD COLUMN fornecedor TEXT DEFAULT 'Não Informado';"); } catch (Exception ignored) {}
            
            // NÍVEL 3 - COLUNA DE JUROS
            try { stmt.execute("ALTER TABLE Vendas ADD COLUMN juros REAL DEFAULT 0.0;"); } catch (Exception ignored) {}

            // 3. Garante que os registros primários existam
            stmt.execute(sqlAdmin);
            stmt.execute(sqlConfigInicial);
            
            System.out.println("Banco de Dados ERP Completo verificado/criado com sucesso!");
            
        } catch (Exception e) {
            System.out.println("Erro ao criar as tabelas do ERP: " + e.getMessage());
        }
    }
}