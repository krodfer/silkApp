package com.example.ufabcirco.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Post implements Serializable {
    private final String id;
    private final String url;
    private final List<String> tags;
    private final String movimentoNome;

    public Post(String url, List<String> tags, String movimentoNome, int movimentoTipo, int movimentoDificuldade) {
        this.id = UUID.randomUUID().toString();
        this.url = url;
        this.tags = tags;
        this.movimentoNome = movimentoNome;
    }

    public String getId() { return id; }
    public String getUrl() { return url; }
    public List<String> getTags() { return tags; }
    public String getMovimentoNome() { return movimentoNome; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equals(url, post.url) && Objects.equals(movimentoNome, post.movimentoNome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, movimentoNome);
    }
}