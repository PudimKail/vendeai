package com.pudimkail.vendeai.avaliacao;

import com.pudimkail.vendeai.produto.Produto;
import com.pudimkail.vendeai.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/** Uma avaliação por comprador e produto. */
@Entity
@Table(name = "avaliacoes", uniqueConstraints = @UniqueConstraint(columnNames = {"produto_id", "autor_id"}))
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Produto produto;

    @ManyToOne(optional = false)
    private Usuario autor;

    private int nota;

    @Column(length = 1000)
    private String comentario;

    private LocalDateTime criadoEm = LocalDateTime.now();

    protected Avaliacao() {
    }

    public Avaliacao(Produto produto, Usuario autor, int nota, String comentario) {
        this.produto = produto;
        this.autor = autor;
        this.nota = nota;
        this.comentario = comentario == null ? "" : comentario.trim();
    }

    public Long getId() { return id; }
    public Produto getProduto() { return produto; }
    public Usuario getAutor() { return autor; }
    public int getNota() { return nota; }
    public String getComentario() { return comentario; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
