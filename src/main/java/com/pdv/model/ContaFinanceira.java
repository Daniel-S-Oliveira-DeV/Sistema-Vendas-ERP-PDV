package com.pdv.model;

public class ContaFinanceira {
    private int id;
    private String tipo;      // ENTRADA ou SAIDA
    private String categoria;
    private String descricao;
    private double valorTotal;
    private double valorPago;
    private String dataVencimento;
    private String dataPagamento;
    private String status;    // PENDENTE ou PAGO
    private int clienteId;

    public ContaFinanceira(int id, String tipo, String categoria, String descricao, double valorTotal, double valorPago, String dataVencimento, String dataPagamento, String status, int clienteId) {
        this.id = id;
        this.tipo = tipo;
        this.categoria = categoria;
        this.descricao = descricao;
        this.valorTotal = valorTotal;
        this.valorPago = valorPago;
        this.dataVencimento = dataVencimento;
        this.dataPagamento = dataPagamento;
        this.status = status;
        this.clienteId = clienteId;
    }

    public int getId() { return id; }
    public String getTipo() { return tipo; }
    public String getCategoria() { return categoria; }
    public String getDescricao() { return descricao; }
    public double getValorTotal() { return valorTotal; }
    public double getValorPago() { return valorPago; }
    public String getDataVencimento() { return dataVencimento; }
    public String getDataPagamento() { return dataPagamento; }
    public String getStatus() { return status; }
    public int getClienteId() { return clienteId; }
    
    public double getSaldoDevedor() {
        return valorTotal - valorPago;
    }
}