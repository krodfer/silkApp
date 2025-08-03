package com.example.ufabcirco.model;

import java.io.Serializable;
import java.util.Objects;

public class Movimento implements Serializable {
    private String nome;
    private int tipo;
    private int dificuldade;

    public Movimento(String nome, int tipo, int dificuldade) {
        this.nome = nome;
        this.tipo = tipo;
        this.dificuldade = dificuldade;
    }

    public String getNome() {
        return nome;
    }

    public int getTipo() {
        return tipo;
    }

    public int getDificuldade() {
        return dificuldade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movimento movimento = (Movimento) o;
        return tipo == movimento.tipo && dificuldade == movimento.dificuldade && Objects.equals(nome, movimento.nome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome, tipo, dificuldade);
    }
}