package com.example.ufabcirco.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Movimento implements Serializable {
    private String nome;
    private int tipo;
    private List<Integer> dificuldades;
    private List<String> fotos;
    private String texto;
    private List<String> variantes;

    public Movimento(String nome, int tipo) {
        this.nome = nome;
        this.tipo = tipo;
        this.dificuldades = new ArrayList<>();
        this.fotos = new ArrayList<>();
        this.variantes = new ArrayList<>();
        this.texto = "";
    }

    public Movimento(String nome, int tipo, List<Integer> dificuldades, List<String> fotos, String texto, List<String> variantes) {
        this.nome = nome;
        this.tipo = tipo;
        this.dificuldades = dificuldades != null ? new ArrayList<>(dificuldades) : new ArrayList<>();
        this.fotos = fotos != null ? new ArrayList<>(fotos) : new ArrayList<>();
        this.texto = texto;
        this.variantes = variantes != null ? new ArrayList<>(variantes) : new ArrayList<>();
    }

    public Movimento(String nome, int tipo, List<Integer> dificuldades) {
        this.nome = nome;
        this.tipo = tipo;
        this.dificuldades = dificuldades != null ? new ArrayList<>(dificuldades) : new ArrayList<>();
        this.fotos = new ArrayList<>();
        this.variantes = new ArrayList<>();
        this.texto = "";
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

    public List<String> getFotos() {
        return fotos;
    }

    public String getTexto() {
        return texto;
    }

    public List<String> getVariantes() {
        return variantes;
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

    public void setFotos(List<String> fotos) {
        this.fotos = fotos;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public void setVariantes(List<String> variantes) {
        this.variantes = variantes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movimento movimento = (Movimento) o;
        return tipo == movimento.tipo && Objects.equals(nome, movimento.nome) && Objects.equals(dificuldades, movimento.dificuldades) && Objects.equals(fotos, movimento.fotos) && Objects.equals(texto, movimento.texto) && Objects.equals(variantes, movimento.variantes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome, tipo, dificuldades, fotos, texto, variantes);
    }
}