package com.pdv.model;

public class Transacao {
    private int id;
    private String tipo;
    private String categoria;
    private String descricao;
    private double valor;
    private double valorPago; // Nível 2 - Baixa Parcial
    private String dataVencimento;
    private String dataPagamento;
    private String status;
    private int clienteId; // Nível 2 - Para liberar o Fiado

    public Transacao(int id, String tipo, String categoria, String descricao, double valor, double valorPago, String dataVencimento, String dataPagamento, String status, int clienteId) {
        this.id = id;
        this.tipo = tipo;
        this.categoria = categoria;
        this.descricao = descricao;
        this.valor = valor;
        this.valorPago = valorPago;
        this.dataVencimento = dataVencimento;
        this.dataPagamento = dataPagamento;
        this.status = status;
        this.clienteId = clienteId;
    }
    
    // Construtor antigo para manter seu código funcionando sem quebrar
    public Transacao(int id, String tipo, String categoria, String descricao, double valor, String dataVencimento, String status) {
        this(id, tipo, categoria, descricao, valor, (status.equals("PAGO") ? valor : 0.0), dataVencimento, null, status, 0);
    }

    public int getId() { return id; }
    public String getTipo() { return tipo; }
    public String getCategoria() { return categoria; }
    public String getDescricao() { return descricao; }
    public double getValor() { return valor; }
    public double getValorPago() { return valorPago; }
    public String getDataVencimento() { return dataVencimento; }
    public String getDataPagamento() { return dataPagamento; }
    public String getStatus() { return status; }
    public int getClienteId() { return clienteId; }

    public double getSaldoDevedor() {
        return valor - valorPago;
    }
}