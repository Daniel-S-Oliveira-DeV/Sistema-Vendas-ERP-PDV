package com.pdv.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pdv.dao.ConexaoSQLite;
import com.pdv.model.RelatorioGerado;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class RelatoriosController {

    @FXML private DatePicker dpDataInicial;
    @FXML private DatePicker dpDataFinal;
    @FXML private Label lblStatus;

    @FXML private TableView<RelatorioGerado> tabelaHistorico;
    @FXML private TableColumn<RelatorioGerado, String> colDataGeracao;
    @FXML private TableColumn<RelatorioGerado, String> colTipo;
    @FXML private TableColumn<RelatorioGerado, String> colPeriodo;
    
    private ObservableList<RelatorioGerado> listaHistorico = FXCollections.observableArrayList();
    
    // Pasta raiz principal
    private final String PASTA_RAIZ = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "TechStoreERP_Relatorios";

    @FXML
    public void initialize() {
        dpDataInicial.setValue(LocalDate.now().withDayOfMonth(1));
        dpDataFinal.setValue(LocalDate.now());

        colDataGeracao.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDataGeracao()));
        colTipo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTipo()));
        colPeriodo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPeriodo()));
        tabelaHistorico.setItems(listaHistorico);
        
        carregarHistorico();
    }
    
    private String prepararSubPasta(String nomePasta) {
        String caminhoSubPasta = PASTA_RAIZ + File.separator + nomePasta;
        File pasta = new File(caminhoSubPasta);
        if (!pasta.exists()) {
            pasta.mkdirs();
        }
        return caminhoSubPasta;
    }

    private void carregarHistorico() {
        listaHistorico.clear();
        String sql = "SELECT id, tipo, periodo, data_geracao, caminho_arquivo FROM Historico_Relatorios ORDER BY id DESC";
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                listaHistorico.add(new RelatorioGerado(
                        rs.getInt("id"), rs.getString("tipo"), rs.getString("periodo"),
                        rs.getString("data_geracao"), rs.getString("caminho_arquivo")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String gerarCaminhoArquivo(String subPasta, String prefixo) {
        String diretorio = prepararSubPasta(subPasta);
        String de = dpDataInicial.getValue().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String ate = dpDataFinal.getValue().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String hora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmm"));
        return diretorio + File.separator + prefixo + "_" + de + "_a_" + ate + "_" + hora + ".pdf";
    }

    // =========================================================================
    // RELATÓRIO DE VENDAS DETALHADO (ITENS VENDIDOS, CUSTO E LUCRO OBTIDO)
    // =========================================================================
    @FXML
    public void gerarRelatorioVendas(ActionEvent event) {
        if (!validarDatas()) return;
        String caminho = gerarCaminhoArquivo("Vendas", "Vendas");
        String sqlVendas = "SELECT id, data_venda, subtotal, desconto, juros, total, forma_pagamento, status " +
                           "FROM Vendas WHERE date(data_venda) BETWEEN ? AND ? ORDER BY id ASC";
        String sqlItens = "SELECT IV.quantidade, IV.preco_unitario, IV.subtotal, P.nome, P.preco_custo, P.unidade " +
                          "FROM Itens_Venda IV JOIN Produtos P ON IV.produto_id = P.id WHERE IV.venda_id = ?";
        
        try (Connection conn = ConexaoSQLite.conectar(); 
             PreparedStatement stmtVendas = conn.prepareStatement(sqlVendas);
             PreparedStatement stmtItens = conn.prepareStatement(sqlItens)) {
            
            stmtVendas.setString(1, dpDataInicial.getValue().toString());
            stmtVendas.setString(2, dpDataFinal.getValue().toString());
            ResultSet rsVendas = stmtVendas.executeQuery();

            // Usa folha A4 em Paisagem (Landscape) para caber todas as colunas com folga
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(caminho));
            document.open();
            adicionarCabecalho(document, "Relatório Detalhado de Vendas, Custos e Lucro (PDV)");

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.9f, 1.5f, 3.6f, 1.4f, 1.3f, 1.3f, 1.3f});
            
            table.addCell(criarCelulaCabecalho("ID"));
            table.addCell(criarCelulaCabecalho("Data / Hora"));
            table.addCell(criarCelulaCabecalho("Produtos Vendidos (Qtd | Venda | Custo)"));
            table.addCell(criarCelulaCabecalho("Pagamento"));
            table.addCell(criarCelulaCabecalho("Custo Total"));
            table.addCell(criarCelulaCabecalho("Total Venda"));
            table.addCell(criarCelulaCabecalho("Lucro Obtido"));

            double faturamentoTotal = 0;
            double custoGeral = 0;
            double lucroGeral = 0;
            Font fonteItens = new Font(Font.HELVETICA, 9, Font.NORMAL);

            while (rsVendas.next()) {
                int vendaId = rsVendas.getInt("id");
                String statusVenda = rsVendas.getString("status");
                boolean cancelada = "CANCELADA".equalsIgnoreCase(statusVenda);

                stmtItens.setInt(1, vendaId);
                ResultSet rsItens = stmtItens.executeQuery();

                StringBuilder detalhesItens = new StringBuilder();
                double custoTotalVenda = 0;

                while (rsItens.next()) {
                    int qtd = rsItens.getInt("quantidade");
                    String nomeProd = rsItens.getString("nome");
                    String un = rsItens.getString("unidade") != null ? rsItens.getString("unidade") : "UN";
                    double precoVendaUnit = rsItens.getDouble("preco_unitario");
                    double precoCustoUnit = rsItens.getDouble("preco_custo");
                    
                    custoTotalVenda += (qtd * precoCustoUnit);
                    
                    detalhesItens.append(qtd).append(" ").append(un).append(" - ").append(nomeProd)
                                 .append(String.format(" (Venda: R$ %.2f | Custo Un: R$ %.2f)\n", precoVendaUnit, precoCustoUnit));
                }

                double valorVenda = rsVendas.getDouble("total");
                double lucroVenda = valorVenda - custoTotalVenda;

                String infoPag = rsVendas.getString("forma_pagamento");
                if (cancelada) {
                    infoPag += " [CANCELADA]";
                } else {
                    faturamentoTotal += valorVenda;
                    custoGeral += custoTotalVenda;
                    lucroGeral += lucroVenda;
                }

                table.addCell(new Paragraph(String.valueOf(vendaId), fonteItens));
                table.addCell(new Paragraph(rsVendas.getString("data_venda"), fonteItens));
                table.addCell(new Paragraph(detalhesItens.length() > 0 ? detalhesItens.toString().trim() : "Sem itens registrados", fonteItens));
                table.addCell(new Paragraph(infoPag, fonteItens));
                table.addCell(new Paragraph(String.format("R$ %.2f", custoTotalVenda), fonteItens));
                table.addCell(new Paragraph(String.format("R$ %.2f", valorVenda), fonteItens));
                table.addCell(new Paragraph(cancelada ? "R$ 0,00 (Estornado)" : String.format("R$ %.2f", lucroVenda), fonteItens));
            }
            
            document.add(table);
            
            Font fonteResumo = new Font(Font.HELVETICA, 12, Font.BOLD);
            document.add(new Paragraph("\nRESUMO FINANCEIRO DAS VENDAS CONCLUÍDAS NO PERÍODO:", fonteResumo));
            document.add(new Paragraph(String.format("• Faturamento Bruto Total (Vendas): R$ %.2f", faturamentoTotal), fonteResumo));
            document.add(new Paragraph(String.format("• Custo Total dos Produtos Vendidos (CMV): R$ %.2f", custoGeral), fonteResumo));
            document.add(new Paragraph(String.format("• Lucro Líquido Total Obtido: R$ %.2f", lucroGeral), new Font(Font.HELVETICA, 14, Font.BOLD)));
            
            document.close();
            
            registrarNoBanco("Vendas (PDV)", obterStringPeriodo(), caminho);
            sucesso("Relatório Gerado com Sucesso!");
            abrirNoSistemaOperacional(caminho);
        } catch (Exception e) { erro(e.getMessage()); }
    }

    @FXML
    public void gerarRelatorioFinanceiro(ActionEvent event) {
        if (!validarDatas()) return;
        String caminho = gerarCaminhoArquivo("Financeiro", "Financeiro");
        String sql = "SELECT tipo, categoria, descricao, valor, status FROM Financeiro WHERE data_vencimento BETWEEN ? AND ? ORDER BY tipo, data_vencimento";
        
        try (Connection conn = ConexaoSQLite.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dpDataInicial.getValue().toString());
            stmt.setString(2, dpDataFinal.getValue().toString());
            ResultSet rs = stmt.executeQuery();

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(caminho));
            document.open();
            adicionarCabecalho(document, "Relatório Financeiro (Caixa)");

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.addCell(criarCelulaCabecalho("Tipo"));
            table.addCell(criarCelulaCabecalho("Categoria"));
            table.addCell(criarCelulaCabecalho("Descrição"));
            table.addCell(criarCelulaCabecalho("Status"));
            table.addCell(criarCelulaCabecalho("Valor"));

            double saldo = 0;
            while (rs.next()) {
                String tipo = rs.getString("tipo");
                double valor = rs.getDouble("valor");
                table.addCell(tipo);
                table.addCell(rs.getString("categoria"));
                table.addCell(rs.getString("descricao"));
                table.addCell(rs.getString("status"));
                table.addCell(String.format("R$ %.2f", valor));
                
                if(rs.getString("status").equals("PAGO")) {
                    if (tipo.equals("ENTRADA")) saldo += valor;
                    else saldo -= valor;
                }
            }
            document.add(table);
            document.add(new Paragraph("\nSaldo Efetivo no Período (PAGOS): R$ " + String.format("%.2f", saldo), new Font(Font.HELVETICA, 14, Font.BOLD)));
            document.close();
            
            registrarNoBanco("Financeiro (Caixa)", obterStringPeriodo(), caminho);
            sucesso("Relatório Financeiro Gerado!");
            abrirNoSistemaOperacional(caminho);
        } catch (Exception e) { erro(e.getMessage()); }
    }

    @FXML
    public void gerarRelatorioEstoque(ActionEvent event) {
        if (!validarDatas()) return;
        String caminho = gerarCaminhoArquivo("Estoque", "MovEstoque");
        String sql = "SELECT M.data_movimentacao, P.nome, M.tipo, M.quantidade, M.motivo " +
                     "FROM Movimentacao_Estoque M JOIN Produtos P ON M.produto_id = P.id " +
                     "WHERE date(M.data_movimentacao) BETWEEN ? AND ? ORDER BY M.data_movimentacao DESC";
        
        try (Connection conn = ConexaoSQLite.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dpDataInicial.getValue().toString());
            stmt.setString(2, dpDataFinal.getValue().toString());
            ResultSet rs = stmt.executeQuery();

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(caminho));
            document.open();
            adicionarCabecalho(document, "Relatório de Movimentação de Estoque");

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.addCell(criarCelulaCabecalho("Data"));
            table.addCell(criarCelulaCabecalho("Produto"));
            table.addCell(criarCelulaCabecalho("Tipo"));
            table.addCell(criarCelulaCabecalho("Qtd"));
            table.addCell(criarCelulaCabecalho("Motivo"));

            while (rs.next()) {
                table.addCell(rs.getString("data_movimentacao"));
                table.addCell(rs.getString("nome"));
                table.addCell(rs.getString("tipo"));
                table.addCell(String.valueOf(rs.getInt("quantidade")));
                table.addCell(rs.getString("motivo"));
            }
            document.add(table);
            document.close();
            
            registrarNoBanco("Mov. Estoque", obterStringPeriodo(), caminho);
            sucesso("Relatório de Estoque Gerado!");
            abrirNoSistemaOperacional(caminho);
        } catch (Exception e) { erro(e.getMessage()); }
    }

    // =========================================================================
    // RELATÓRIO DE PERDAS DETALHADO (DADOS COMPLETOS DO PRODUTO, UNITÁRIO E TOTAL)
    // =========================================================================
    @FXML
    public void gerarRelatorioPerdas(ActionEvent event) {
        if (!validarDatas()) return;
        String caminho = gerarCaminhoArquivo("Perdas", "PerdasAjustes");
        String sql = "SELECT M.data_movimentacao, P.id, P.codigo_barras, P.nome, P.categoria, P.unidade, P.fornecedor, " +
                     "M.quantidade, M.motivo, P.preco_custo, P.preco_venda " +
                     "FROM Movimentacao_Estoque M JOIN Produtos P ON M.produto_id = P.id " +
                     "WHERE M.tipo = 'SAIDA' AND M.motivo NOT LIKE 'Venda %' AND date(M.data_movimentacao) BETWEEN ? AND ? " +
                     "ORDER BY M.data_movimentacao DESC";
        
        try (Connection conn = ConexaoSQLite.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dpDataInicial.getValue().toString());
            stmt.setString(2, dpDataFinal.getValue().toString());
            ResultSet rs = stmt.executeQuery();

            // Usa folha A4 em Paisagem (Landscape) para acomodar todas as informações do produto
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, new FileOutputStream(caminho));
            document.open();
            adicionarCabecalho(document, "Relatório Detalhado de Perdas e Ajustes (Saídas Manuais)");

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.4f, 1.1f, 2.5f, 1.0f, 1.2f, 1.2f, 1.3f, 1.8f});
            
            table.addCell(criarCelulaCabecalho("Data"));
            table.addCell(criarCelulaCabecalho("Código"));
            table.addCell(criarCelulaCabecalho("Produto / Categoria"));
            table.addCell(criarCelulaCabecalho("Qtd Perdida"));
            table.addCell(criarCelulaCabecalho("Custo Unit."));
            table.addCell(criarCelulaCabecalho("Venda Unit."));
            table.addCell(criarCelulaCabecalho("Custo Total"));
            table.addCell(criarCelulaCabecalho("Motivo"));

            double prejuizoCustoTotal = 0;
            double perdaVendaTotal = 0;
            Font fonteCelula = new Font(Font.HELVETICA, 9, Font.NORMAL);

            while (rs.next()) {
                String codigo = rs.getString("codigo_barras") != null && !rs.getString("codigo_barras").isEmpty()
                        ? rs.getString("codigo_barras") : String.valueOf(rs.getInt("id"));
                String nome = rs.getString("nome");
                String categoria = rs.getString("categoria") != null ? rs.getString("categoria") : "Geral";
                String unidade = rs.getString("unidade") != null ? rs.getString("unidade") : "UN";
                String fornecedor = rs.getString("fornecedor") != null && !rs.getString("fornecedor").isEmpty()
                        ? " | Forn: " + rs.getString("fornecedor") : "";

                int qtd = rs.getInt("quantidade");
                double custoUnit = rs.getDouble("preco_custo");
                double vendaUnit = rs.getDouble("preco_venda");
                double custoTotalLinha = qtd * custoUnit;
                double vendaTotalLinha = qtd * vendaUnit;

                prejuizoCustoTotal += custoTotalLinha;
                perdaVendaTotal += vendaTotalLinha;

                table.addCell(new Paragraph(rs.getString("data_movimentacao"), fonteCelula));
                table.addCell(new Paragraph(codigo, fonteCelula));
                table.addCell(new Paragraph(nome + "\n(" + categoria + fornecedor + ")", fonteCelula));
                table.addCell(new Paragraph(qtd + " " + unidade, fonteCelula));
                table.addCell(new Paragraph(String.format("R$ %.2f", custoUnit), fonteCelula));
                table.addCell(new Paragraph(String.format("R$ %.2f", vendaUnit), fonteCelula));
                table.addCell(new Paragraph(String.format("R$ %.2f", custoTotalLinha), fonteCelula));
                table.addCell(new Paragraph(rs.getString("motivo"), fonteCelula));
            }
            document.add(table);
            
            Font fonteResumo = new Font(Font.HELVETICA, 12, Font.BOLD);
            document.add(new Paragraph("\nRESUMO DE IMPACTO FINANCEIRO DAS PERDAS:", fonteResumo));
            document.add(new Paragraph(String.format("• Prejuízo Real de Estoque (Baseado no Custo Total): R$ %.2f", prejuizoCustoTotal), fonteResumo));
            document.add(new Paragraph(String.format("• Valor Total a Preço de Venda (Receita Cessante): R$ %.2f", perdaVendaTotal), fonteResumo));
            
            document.close();
            
            registrarNoBanco("Perdas / Ajustes", obterStringPeriodo(), caminho);
            sucesso("Relatório de Perdas Gerado!");
            abrirNoSistemaOperacional(caminho);
        } catch (Exception e) { erro(e.getMessage()); }
    }

    @FXML
    public void gerarRelatorioFiados(ActionEvent event) {
        if (!validarDatas()) return;
        String caminho = gerarCaminhoArquivo("Fiados", "FiadosPendentes");
        String sql = "SELECT descricao, valor FROM Financeiro WHERE categoria = 'Fiado / Caderninho' AND status = 'PENDENTE' AND data_vencimento BETWEEN ? AND ?";
        
        try (Connection conn = ConexaoSQLite.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dpDataInicial.getValue().toString());
            stmt.setString(2, dpDataFinal.getValue().toString());
            ResultSet rs = stmt.executeQuery();

            Map<String, double[]> fiadosPorCliente = new HashMap<>();

            while (rs.next()) {
                String descricao = rs.getString("descricao");
                double valor = rs.getDouble("valor");
                String cliente = extrairNomeCliente(descricao);

                fiadosPorCliente.putIfAbsent(cliente, new double[]{0, 0});
                fiadosPorCliente.get(cliente)[0] += 1; 
                fiadosPorCliente.get(cliente)[1] += valor;
            }

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(caminho));
            document.open();
            adicionarCabecalho(document, "Relatório Consolidado de Fiados (Pendentes)");

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.addCell(criarCelulaCabecalho("Nome do Cliente"));
            table.addCell(criarCelulaCabecalho("Qtd de Notas (Compras)"));
            table.addCell(criarCelulaCabecalho("Dívida Total"));

            double dividaGeral = 0;
            for (Map.Entry<String, double[]> entry : fiadosPorCliente.entrySet()) {
                table.addCell(entry.getKey());
                table.addCell(String.valueOf((int) entry.getValue()[0]));
                double totalCliente = entry.getValue()[1];
                dividaGeral += totalCliente;
                table.addCell(String.format("R$ %.2f", totalCliente));
            }
            
            document.add(table);
            document.add(new Paragraph("\nTotal Preso em Fiados no Período: R$ " + String.format("%.2f", dividaGeral), new Font(Font.HELVETICA, 14, Font.BOLD)));
            document.close();
            
            registrarNoBanco("Fiados (Agrupados)", obterStringPeriodo(), caminho);
            sucesso("Relatório de Fiados Gerado!");
            abrirNoSistemaOperacional(caminho);
        } catch (Exception e) { erro(e.getMessage()); }
    }

    private String extrairNomeCliente(String descricao) {
        try {
            if (descricao.startsWith("Fiado: ")) {
                int idx = descricao.indexOf(" (Venda");
                if (idx > 0) return descricao.substring(7, idx).trim();
                return descricao.substring(7).trim();
            }
        } catch (Exception e) {}
        return descricao;
    }

    @FXML
    public void abrirPastaRelatorios(ActionEvent event) {
        abrirNoSistemaOperacional(PASTA_RAIZ);
    }

    @FXML
    public void abrirRelatorioSelecionado(ActionEvent event) {
        RelatorioGerado selecionado = tabelaHistorico.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            abrirNoSistemaOperacional(selecionado.getCaminhoArquivo());
        } else {
            erro("Selecione um relatório na tabela primeiro.");
        }
    }

    @FXML
    public void limparHistorico(ActionEvent event) {
        String sql = "DELETE FROM Historico_Relatorios";
        try (Connection conn = ConexaoSQLite.conectar();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
            carregarHistorico();
            sucesso("Os registros antigos foram limpos da tabela.");
        } catch (Exception e) {
            erro("Erro ao limpar histórico: " + e.getMessage());
        }
    }

    private void abrirNoSistemaOperacional(String caminho) {
        File arquivoOuPasta = new File(caminho);
        if (!arquivoOuPasta.exists()) {
            javafx.application.Platform.runLater(() -> erro("O arquivo não existe mais na pasta:\n" + caminho));
            return;
        }
        new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    if (arquivoOuPasta.isDirectory()) new ProcessBuilder("explorer", arquivoOuPasta.getAbsolutePath()).start();
                    else new ProcessBuilder("cmd", "/c", "start", "", arquivoOuPasta.getAbsolutePath()).start();
                } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[]{"xdg-open", arquivoOuPasta.getAbsolutePath()});
                } else {
                    java.awt.Desktop.getDesktop().open(arquivoOuPasta);
                }
            } catch (Exception ex) {
                System.out.println("Erro ao abrir no SO: " + ex.getMessage());
            }
        }).start();
    }

    private void registrarNoBanco(String tipo, String periodo, String caminho) {
        String dataGeracao = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String sql = "INSERT INTO Historico_Relatorios (tipo, periodo, data_geracao, caminho_arquivo) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConexaoSQLite.conectar(); PreparedStatement stmt = conn.prepareStatement(sql)) {
             stmt.setString(1, tipo);
             stmt.setString(2, periodo);
             stmt.setString(3, dataGeracao);
             stmt.setString(4, caminho);
             stmt.executeUpdate();
             carregarHistorico();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    private void sucesso(String msg) {
        lblStatus.setStyle("-fx-text-fill: #2ed573;");
        lblStatus.setText(msg);
    }
    
    private void erro(String msg) {
        lblStatus.setStyle("-fx-text-fill: #ffce00;");
        lblStatus.setText(msg);
    }

    private String obterStringPeriodo() {
        return dpDataInicial.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " a " + dpDataFinal.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private boolean validarDatas() {
        if (dpDataInicial.getValue() == null || dpDataFinal.getValue() == null) {
            lblStatus.setStyle("-fx-text-fill: #ffce00;");
            lblStatus.setText("Selecione a data inicial e final.");
            return false;
        }
        lblStatus.setText("");
        return true;
    }

    // =========================================================================
    // CABEÇALHO WHITE-LABEL NOS RELATÓRIOS (NOME E CNPJ DO CLIENTE FINAL)
    // =========================================================================
    private void adicionarCabecalho(Document document, String titulo) throws Exception {
        String nomeEmpresa = "Sistema de Vendas";
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
        } catch (Exception e) {}

        Font fonteTitulo = new Font(Font.HELVETICA, 16, Font.BOLD);
        Paragraph pTitulo = new Paragraph(nomeEmpresa + " - " + titulo, fonteTitulo);
        pTitulo.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(pTitulo);

        if (!docEmpresa.isEmpty()) {
            Paragraph pDoc = new Paragraph("CNPJ / CPF: " + docEmpresa, new Font(Font.HELVETICA, 11, Font.NORMAL));
            pDoc.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(pDoc);
        }

        Paragraph pPeriodo = new Paragraph("Período Abordado: " + obterStringPeriodo() + "\n\n");
        pPeriodo.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(pPeriodo);
    }

    private PdfPCell criarCelulaCabecalho(String texto) {
        PdfPCell celula = new PdfPCell(new Paragraph(texto, new Font(Font.HELVETICA, 10, Font.BOLD)));
        celula.setPadding(6);
        return celula;
    }
}