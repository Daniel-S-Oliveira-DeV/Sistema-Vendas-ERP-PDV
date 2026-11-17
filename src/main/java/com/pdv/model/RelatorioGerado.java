package com.pdv.model;

public class RelatorioGerado {
    private int id;
    private String tipo;
    private String periodo;
    private String dataGeracao;
    private String caminhoArquivo;

    public RelatorioGerado(int id, String tipo, String periodo, String dataGeracao, String caminhoArquivo) {
        this.id = id;
        this.tipo = tipo;
        this.periodo = periodo;
        this.dataGeracao = dataGeracao;
        this.caminhoArquivo = caminhoArquivo;
    }

    public int getId() { return id; }
    public String getTipo() { return tipo; }
    public String getPeriodo() { return periodo; }
    public String getDataGeracao() { return dataGeracao; }
    public String getCaminhoArquivo() { return caminhoArquivo; }
}