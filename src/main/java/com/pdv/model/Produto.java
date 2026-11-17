package com.pdv.model;

public class Produto {
    private int id;
    private String codigoBarras;
    private String nome;
    private String unidade;
    private String ncm;
    private double precoCusto;
    private double precoVenda;
    private int estoqueAtual;
    private int estoqueMinimo;
    
    // Nível 2 - Campos Adicionais
    private String categoria;
    private String fornecedor;

    public Produto(int id, String codigoBarras, String nome, String unidade, String ncm, double precoCusto, double precoVenda, int estoqueAtual, int estoqueMinimo, String categoria, String fornecedor) {
        this.id = id;
        this.codigoBarras = codigoBarras;
        this.nome = nome;
        this.unidade = unidade;
        this.ncm = ncm;
        this.precoCusto = precoCusto;
        this.precoVenda = precoVenda;
        this.estoqueAtual = estoqueAtual;
        this.estoqueMinimo = estoqueMinimo;
        this.categoria = categoria;
        this.fornecedor = fornecedor;
    }
    
    // Construtor antigo para manter retrocompatibilidade se necessário
    public Produto(int id, String codigoBarras, String nome, String unidade, String ncm, double precoCusto, double precoVenda, int estoqueAtual, int estoqueMinimo) {
        this(id, codigoBarras, nome, unidade, ncm, precoCusto, precoVenda, estoqueAtual, estoqueMinimo, "Geral", "Não Informado");
    }

    public int getId() { return id; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getNome() { return nome; }
    public String getUnidade() { return unidade; }
    public String getNcm() { return ncm; }
    public double getPrecoCusto() { return precoCusto; }
    public double getPrecoVenda() { return precoVenda; }
    public int getEstoqueAtual() { return estoqueAtual; }
    public int getEstoqueMinimo() { return estoqueMinimo; }
    public String getCategoria() { return categoria; }
    public String getFornecedor() { return fornecedor; }
}