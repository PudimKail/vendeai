package com.pudimkail.vendeai.produto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Formulário de "Vender" (criar e editar anúncio). */
public class AnuncioForm {

    @NotBlank(message = "Informe o título")
    @Size(min = 5, max = 120, message = "O título precisa ter de 5 a 120 caracteres")
    private String titulo;

    @Size(max = 4000, message = "A descrição pode ter até 4000 caracteres")
    private String descricao;

    @NotNull(message = "Informe o preço")
    @DecimalMin(value = "1.00", message = "O preço mínimo é R$ 1,00")
    @DecimalMax(value = "999999.99", message = "Preço muito alto")
    private BigDecimal preco;

    @Min(value = 1, message = "O estoque precisa ser pelo menos 1")
    @Max(value = 9999, message = "Estoque máximo de 9999")
    private int estoque = 1;

    @NotNull(message = "Escolha a condição")
    private Produto.Condicao condicao = Produto.Condicao.NOVO;

    @NotNull(message = "Escolha a categoria")
    private Long categoriaId;

    private boolean freteGratis;

    public static AnuncioForm de(Produto p) {
        AnuncioForm f = new AnuncioForm();
        f.titulo = p.getTitulo();
        f.descricao = p.getDescricao();
        f.preco = p.getPreco();
        f.estoque = p.getEstoque();
        f.condicao = p.getCondicao();
        f.categoriaId = p.getCategoria().getId();
        f.freteGratis = p.isFreteGratis();
        return f;
    }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }
    public int getEstoque() { return estoque; }
    public void setEstoque(int estoque) { this.estoque = estoque; }
    public Produto.Condicao getCondicao() { return condicao; }
    public void setCondicao(Produto.Condicao condicao) { this.condicao = condicao; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public boolean isFreteGratis() { return freteGratis; }
    public void setFreteGratis(boolean freteGratis) { this.freteGratis = freteGratis; }
}
