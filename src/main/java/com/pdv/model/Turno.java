package com.pdv.model;

public class Turno {
    private int id;
    private String dataAbertura;
    private double valorAbertura;
    private String dataFechamento;
    private double valorFechamento;
    private String status;

    public Turno(int id, String dataAbertura, double valorAbertura, String dataFechamento, double valorFechamento, String status) {
        this.id = id;
        this.dataAbertura = dataAbertura;
        this.valorAbertura = valorAbertura;
        this.dataFechamento = dataFechamento;
        this.valorFechamento = valorFechamento;
        this.status = status;
    }

    public int getId() { return id; }
    public String getDataAbertura() { return dataAbertura; }
    public double getValorAbertura() { return valorAbertura; }
    public String getDataFechamento() { return dataFechamento; }
    public double getValorFechamento() { return valorFechamento; }
    public String getStatus() { return status; }
}