package com.pdv.model;

public class ItemVenda {
    private int produtoId;
    private String codigo;
    private String nome;
    private int quantidade;
    private double precoUnitario;
    private double subtotal;

    public ItemVenda(int produtoId, String codigo, String nome, int quantidade, double precoUnitario) {
        this.produtoId = produtoId;
        this.codigo = codigo;
        this.nome = nome;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
        this.subtotal = quantidade * precoUnitario;
    }

    public int getProdutoId() { return produtoId; }
    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public int getQuantidade() { return quantidade; }
    public double getPrecoUnitario() { return precoUnitario; }
    public double getSubtotal() { return subtotal; }
}