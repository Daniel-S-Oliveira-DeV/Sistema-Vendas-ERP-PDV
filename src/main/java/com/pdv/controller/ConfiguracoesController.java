package com.pdv.controller;

import com.pdv.dao.ConexaoSQLite;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

class UsuarioItem {
    int id;
    String nome;
    int nivel;

    public UsuarioItem(int id, String nome, int nivel) {
        this.id = id;
        this.nome = nome;
        this.nivel = nivel;
    }

    public String getDescricaoNivel() {
        return nivel == 1 ? "Nível 1 - Administrador" : "Nível 2 - Operador / Caixa";
    }
}

public class ConfiguracoesController {
    
    @FXML private Label lblStatusBackup;
    
    // Nível 3 - White-Label
    @FXML private TextField txtNomeEmpresa;
    @FXML private TextField txtDocumento;
    @FXML private Label lblStatusConfig;

    // Gerenciamento de Usuários
    @FXML private TextField txtNovoUsuario;
    @FXML private PasswordField txtNovaSenha;
    @FXML private ComboBox<String> cbNivelAcesso;
    @FXML private Label lblStatusUsuario;
    @FXML private TableView<UsuarioItem> tabelaUsuarios;
    @FXML private TableColumn<UsuarioItem, String> colUsuarioId;
    @FXML private TableColumn<UsuarioItem, String> colUsuarioNome;
    @FXML private TableColumn<UsuarioItem, String> colUsuarioNivel;

    private ObservableList<UsuarioItem> listaUsuarios = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        carregarDadosConfiguracao();
        configurarPainelUsuarios();
    }
    
    // ==========================================
    // GERENCIAMENTO DE USUÁRIOS (LOGIN E SENHA)
    // ==========================================
    private void configurarPainelUsuarios() {
        if (cbNivelAcesso != null) {
            cbNivelAcesso.setItems(FXCollections.observableArrayList(
                    "1 - Administrador (Acesso Total)",
                    "2 - Operador / Vendedor (Padrão)"
            ));
            cbNivelAcesso.setValue("2 - Operador / Vendedor (Padrão)");
        }

        if (tabelaUsuarios != null) {
            colUsuarioId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().id)));
            colUsuarioNome.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nome));
            colUsuarioNivel.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescricaoNivel()));
            tabelaUsuarios.setItems(listaUsuarios);

            // Ao clicar em um usuário na tabela, preenche o login para facilitar edição de senha/nível
            tabelaUsuarios.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selecionado) -> {
                if (selecionado != null) {
                    txtNovoUsuario.setText(selecionado.nome);
                    txtNovaSenha.clear();
                    cbNivelAcesso.setValue(selecionado.nivel == 1
                            ? "1 - Administrador (Acesso Total)"
                            : "2 - Operador / Vendedor (Padrão)");
                }
            });

            carregarUsuarios();
        }
    }

    private void carregarUsuarios() {
        listaUsuarios.clear();
        String sql = "SELECT id, nome, nivel_acesso FROM Usuarios WHERE ativo = 1 ORDER BY id ASC";
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                listaUsuarios.add(new UsuarioItem(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getInt("nivel_acesso")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void salvarUsuario(ActionEvent event) {
        String login = txtNovoUsuario.getText().trim();
        String senha = txtNovaSenha.getText();
        int nivel = cbNivelAcesso.getValue() != null && cbNivelAcesso.getValue().startsWith("1") ? 1 : 2;

        if (login.isEmpty() || senha.isEmpty()) {
            mostrarStatusUsuario("Preencha o nome de usuário (login) e a senha.", false);
            return;
        }

        try (Connection conn = ConexaoSQLite.conectar()) {
            // Verifica se o usuário já existe para decidir entre Criar Novo ou Atualizar Senha
            String sqlCheck = "SELECT id FROM Usuarios WHERE nome = ?";
            int idExistente = -1;
            try (PreparedStatement stmtCheck = conn.prepareStatement(sqlCheck)) {
                stmtCheck.setString(1, login);
                ResultSet rs = stmtCheck.executeQuery();
                if (rs.next()) {
                    idExistente = rs.getInt("id");
                }
            }

            if (idExistente != -1) {
                String sqlUpdate = "UPDATE Usuarios SET senha = ?, nivel_acesso = ?, ativo = 1 WHERE id = ?";
                try (PreparedStatement stmtUp = conn.prepareStatement(sqlUpdate)) {
                    stmtUp.setString(1, senha);
                    stmtUp.setInt(2, nivel);
                    stmtUp.setInt(3, idExistente);
                    stmtUp.executeUpdate();
                }
                mostrarStatusUsuario("Senha / permissão do usuário '" + login + "' atualizada com sucesso!", true);
            } else {
                String sqlInsert = "INSERT INTO Usuarios (nome, senha, nivel_acesso, ativo) VALUES (?, ?, ?, 1)";
                try (PreparedStatement stmtIn = conn.prepareStatement(sqlInsert)) {
                    stmtIn.setString(1, login);
                    stmtIn.setString(2, senha);
                    stmtIn.setInt(3, nivel);
                    stmtIn.executeUpdate();
                }
                mostrarStatusUsuario("Novo usuário '" + login + "' cadastrado com sucesso!", true);
            }

            limparCamposUsuario(null);
            carregarUsuarios();

        } catch (Exception e) {
            mostrarStatusUsuario("Erro ao salvar usuário: " + e.getMessage(), false);
        }
    }

    @FXML
    public void excluirUsuario(ActionEvent event) {
        UsuarioItem selecionado = tabelaUsuarios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarStatusUsuario("Selecione um usuário na tabela para excluir.", false);
            return;
        }

        if ("admin".equalsIgnoreCase(selecionado.nome)) {
            mostrarStatusUsuario("Operação bloqueada: O usuário principal 'admin' não pode ser excluído.", false);
            return;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Excluir Usuário");
        alerta.setHeaderText("Remover acesso do usuário: " + selecionado.nome);
        alerta.setContentText("Tem certeza que deseja excluir este usuário?");

        Optional<ButtonType> resp = alerta.showAndWait();
        if (resp.isPresent() && resp.get() == ButtonType.OK) {
            try (Connection conn = ConexaoSQLite.conectar();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM Usuarios WHERE id = ?")) {
                stmt.setInt(1, selecionado.id);
                stmt.executeUpdate();

                limparCamposUsuario(null);
                carregarUsuarios();
                mostrarStatusUsuario("Usuário '" + selecionado.nome + "' removido com sucesso!", true);
            } catch (Exception e) {
                mostrarStatusUsuario("Erro ao remover usuário: " + e.getMessage(), false);
            }
        }
    }

    @FXML
    public void limparCamposUsuario(ActionEvent event) {
        if (txtNovoUsuario != null) txtNovoUsuario.clear();
        if (txtNovaSenha != null) txtNovaSenha.clear();
        if (cbNivelAcesso != null) cbNivelAcesso.setValue("2 - Operador / Vendedor (Padrão)");
        if (tabelaUsuarios != null) tabelaUsuarios.getSelectionModel().clearSelection();
        if (event != null && lblStatusUsuario != null) lblStatusUsuario.setText("");
    }

    private void mostrarStatusUsuario(String msg, boolean sucesso) {
        lblStatusUsuario.setStyle((sucesso ? "-fx-text-fill: #2ed573;" : "-fx-text-fill: #ffce00;") + " -fx-font-weight: bold;");
        lblStatusUsuario.setText(msg);
    }

    // ==========================================
    // WHITE-LABEL (PERSONALIZAÇÃO DA EMPRESA)
    // ==========================================
    private void carregarDadosConfiguracao() {
        String sql = "SELECT nome_empresa, documento_empresa FROM Configuracoes WHERE id = 1";
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                txtNomeEmpresa.setText(rs.getString("nome_empresa"));
                
                String doc = rs.getString("documento_empresa");
                txtDocumento.setText(doc != null ? doc : "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void salvarConfiguracoes(ActionEvent event) {
        String novoNome = txtNomeEmpresa.getText().trim();
        String novoDoc = txtDocumento.getText().trim();
        
        if (novoNome.isEmpty()) {
            lblStatusConfig.setStyle("-fx-text-fill: #ffce00; -fx-font-weight: bold;");
            lblStatusConfig.setText("O nome do sistema não pode ficar em branco.");
            return;
        }

        String sql = "UPDATE Configuracoes SET nome_empresa = ?, documento_empresa = ? WHERE id = 1";
        
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, novoNome);
            stmt.setString(2, novoDoc);
            stmt.executeUpdate();
            
            lblStatusConfig.setStyle("-fx-text-fill: #2ed573; -fx-font-weight: bold;");
            lblStatusConfig.setText("Configurações atualizadas! As mudanças de nome serão visíveis na próxima vez que você abrir ou recarregar o sistema.");
            
        } catch (Exception e) {
            lblStatusConfig.setStyle("-fx-text-fill: #ff5252; -fx-font-weight: bold;");
            lblStatusConfig.setText("Erro ao salvar: " + e.getMessage());
        }
    }
    
    // ==========================================
    // BACKUP E RESTAURAÇÃO
    // ==========================================
    private File encontrarBancoDados() {
        File diretorioAtual = new File(System.getProperty("user.dir"));
        File[] arquivosDB = diretorioAtual.listFiles((dir, nome) -> nome.toLowerCase().endsWith(".db"));
        
        if (arquivosDB != null && arquivosDB.length > 0) {
            return arquivosDB[0]; 
        }
        return new File("techstore.db"); 
    }

    @FXML
    public void exportarBackup(ActionEvent event) {
        File dbFile = encontrarBancoDados();
        if (!dbFile.exists()) {
            mostrarAviso("Banco de dados local não encontrado para realizar o backup.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Cópia de Segurança (Backup)");
        fileChooser.setInitialFileName("Backup_TechStoreERP_" + System.currentTimeMillis() + ".db");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Banco de Dados SQLite (*.db)", "*.db"));
        
        Stage janela = (Stage) lblStatusBackup.getScene().getWindow();
        File destino = fileChooser.showSaveDialog(janela);
        
        if (destino != null) {
            try {
                Files.copy(dbFile.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                mostrarSucesso("Backup gerado com sucesso! Arquivo salvo em:\n" + destino.getAbsolutePath());
            } catch (Exception e) {
                mostrarAviso("Erro ao exportar banco: " + e.getMessage());
            }
        }
    }

    @FXML
    public void importarBackup(ActionEvent event) {
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Atenção Crítica - Importação de Backup");
        alerta.setHeaderText("Você está prestes a SUBSTITUIR o banco de dados atual!");
        alerta.setContentText("Todos os dados do dia de hoje serão APAGADOS e substituídos pelos dados do arquivo que você vai importar.\n\nDeseja realmente continuar?");
        
        Optional<ButtonType> resposta = alerta.showAndWait();
        if (resposta.isPresent() && resposta.get() == ButtonType.OK) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selecione o arquivo de Backup anterior");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Banco de Dados SQLite (*.db)", "*.db"));
            
            Stage janela = (Stage) lblStatusBackup.getScene().getWindow();
            File origem = fileChooser.showOpenDialog(janela);
            
            if (origem != null) {
                File dbFile = encontrarBancoDados();
                try {
                    Files.copy(origem.toPath(), dbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    mostrarSucesso("Backup importado com sucesso! ATENÇÃO: Reinicie o sistema agora para carregar os novos dados.");
                } catch (Exception e) {
                    mostrarAviso("Erro ao importar backup: " + e.getMessage());
                }
            }
        }
    }
    
    private void mostrarSucesso(String msg) {
        lblStatusBackup.setStyle("-fx-text-fill: #2ed573; -fx-font-weight: bold;");
        lblStatusBackup.setText(msg);
    }

    private void mostrarAviso(String msg) {
        lblStatusBackup.setStyle("-fx-text-fill: #ff5252; -fx-font-weight: bold;");
        lblStatusBackup.setText(msg);
    }
}