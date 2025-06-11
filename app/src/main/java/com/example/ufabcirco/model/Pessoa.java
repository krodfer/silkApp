// app/src/main/java/com/example/ufabcirco/model/Pessoa.java
package com.example.ufabcirco.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Pessoa implements Serializable {
    private String id;
    private String nome;
    private Map<String, Integer> moveStatus;

    public Pessoa(String nome) {
        this.id = UUID.randomUUID().toString();
        this.nome = nome;
        this.moveStatus = new HashMap<>();
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public Map<String, Integer> getMoveStatus() {
        if (this.moveStatus == null) {
            this.moveStatus = new HashMap<>();
        }
        return moveStatus;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
    public void setMoveStatus(Map<String, Integer> moveStatus) {
        this.moveStatus = moveStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pessoa pessoa = (Pessoa) o;
        return id.equals(pessoa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}