package com.pdv.model;

public class Cliente {
    private int id;
    private String nome;
    private String cpf;
    private String telefone;
    private String endereco;
    private double limiteCredito;
    private double creditoUsado;

    public Cliente(int id, String nome, String cpf, String telefone, String endereco, double limiteCredito, double creditoUsado) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
        this.endereco = endereco;
        this.limiteCredito = limiteCredito;
        this.creditoUsado = creditoUsado;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getTelefone() { return telefone; }
    public String getEndereco() { return endereco; }
    public double getLimiteCredito() { return limiteCredito; }
    public double getCreditoUsado() { return creditoUsado; }
    
    // Calcula automaticamente o quanto ele ainda pode comprar
    public double getSaldoDisponivel() {
        return limiteCredito - creditoUsado;
    }
}