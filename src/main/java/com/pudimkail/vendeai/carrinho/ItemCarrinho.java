package com.pudimkail.vendeai.carrinho;

import com.pudimkail.vendeai.produto.Produto;
import com.pudimkail.vendeai.usuario.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(name = "carrinho_itens", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "produto_id"}))
public class ItemCarrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    private Produto produto;

    private int quantidade;

    protected ItemCarrinho() {
    }

    public ItemCarrinho(Usuario usuario, Produto produto, int quantidade) {
        this.usuario = usuario;
        this.produto = produto;
        this.quantidade = quantidade;
    }

    public BigDecimal getSubtotal() {
        return produto.getPreco().multiply(BigDecimal.valueOf(quantidade));
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Produto getProduto() { return produto; }
    public int getQuantidade() { return quantidade; }

    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
}
