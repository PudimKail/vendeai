package com.pudimkail.vendeai.produto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String slug;

    /** Emoji usado no ícone da categoria e na foto padrão dos produtos. */
    private String icone;

    protected Categoria() {
    }

    public Categoria(String nome, String slug, String icone) {
        this.nome = nome;
        this.slug = slug;
        this.icone = icone;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getSlug() { return slug; }
    public String getIcone() { return icone; }
}
