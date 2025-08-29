package com.example.ufabcirco.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Movimento implements Serializable {
    private String nome;
    private int tipo;
    private List<Integer> dificuldades;

    public Movimento(String nome, int tipo) {
        this.nome = nome;
        this.tipo = tipo;
        this.dificuldades = new ArrayList<>();
    }

    public Movimento(String nome, int tipo, List<Integer> dificuldades) {
        this.nome = nome;
        this.tipo = tipo;
        this.dificuldades = dificuldades != null ? new ArrayList<>(dificuldades) : new ArrayList<>();
    }

    public Movimento(String nome, int tipo, int dificuldade) {
        this.nome = nome;
        this.tipo = tipo;
        this.dificuldades = new ArrayList<>();
        this.dificuldades.add(dificuldade);
    }

    public String getNome() {
        return nome;
    }

    public int getTipo() {
        return tipo;
    }

    public List<Integer> getDificuldades() {
        return dificuldades;
    }

    public void addDificuldade(int novaDificuldade) {
        if (this.dificuldades == null) {
            this.dificuldades = new ArrayList<>();
        }
        this.dificuldades.add(novaDificuldade);
    }

    public double getMediaDificuldade() {
        if (dificuldades == null || dificuldades.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        for (int d : dificuldades) {
            sum += d;
        }
        return (double) sum / dificuldades.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movimento movimento = (Movimento) o;
        return tipo == movimento.tipo && Objects.equals(nome, movimento.nome) && Objects.equals(dificuldades, movimento.dificuldades);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome, tipo, dificuldades);
    }
}