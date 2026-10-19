package com.pudimkail.vendeai.produto;

import com.pudimkail.vendeai.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "produtos")
public class Produto {

    public static final int PARCELAS_SEM_JUROS = 10;

    public enum Condicao {
        NOVO("Novo"), USADO("Usado");

        private final String rotulo;

        Condicao(String rotulo) { this.rotulo = rotulo; }

        public String getRotulo() { return rotulo; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 4000)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    private int estoque;

    @Enumerated(EnumType.STRING)
    private Condicao condicao = Condicao.NOVO;

    @ManyToOne(optional = false)
    private Categoria categoria;

    @ManyToOne(optional = false)
    private Usuario vendedor;

    /** Nome do arquivo da foto na pasta de uploads. Sem foto, a página mostra o ícone da categoria. */
    private String imagem;

    private boolean freteGratis;
    private int vendidos;
    private double notaMedia;
    private int totalAvaliacoes;
    private boolean ativo = true;
    private LocalDateTime criadoEm = LocalDateTime.now();

    protected Produto() {
    }

    public Produto(String titulo, String descricao, BigDecimal preco, int estoque, Condicao condicao,
                   Categoria categoria, Usuario vendedor, boolean freteGratis) {
        this.vendedor = vendedor;
        atualizar(titulo, descricao, preco, estoque, condicao, categoria, freteGratis);
    }

    public void atualizar(String titulo, String descricao, BigDecimal preco, int estoque, Condicao condicao,
                          Categoria categoria, boolean freteGratis) {
        this.titulo = titulo.trim();
        this.descricao = descricao == null ? "" : descricao.trim();
        this.preco = preco;
        this.estoque = estoque;
        this.condicao = condicao;
        this.categoria = categoria;
        this.freteGratis = freteGratis;
    }

    /** Baixa o estoque quando o pagamento é aprovado. */
    public void vender(int quantidade) {
        if (quantidade > estoque) {
            throw new IllegalStateException("Estoque insuficiente para " + titulo);
        }
        estoque -= quantidade;
        vendidos += quantidade;
    }

    /** Atualiza a média sem precisar recalcular todas as avaliações. */
    public void registrarAvaliacao(int nota) {
        notaMedia = (notaMedia * totalAvaliacoes + nota) / (totalAvaliacoes + 1);
        totalAvaliacoes++;
    }

    /** Ex.: média 4,2 -> "★★★★☆" */
    public String getEstrelas() {
        int cheias = (int) Math.round(notaMedia);
        return "★".repeat(cheias) + "☆".repeat(5 - cheias);
    }

    public String getNotaFormatada() {
        return String.format(java.util.Locale.of("pt", "BR"), "%.1f", notaMedia);
    }

    public boolean isDisponivel() {
        return ativo && estoque > 0;
    }

    public BigDecimal getValorParcela() {
        return preco.divide(BigDecimal.valueOf(PARCELAS_SEM_JUROS), 2, RoundingMode.HALF_UP);
    }

    public boolean pertenceA(Usuario usuario) {
        return usuario != null && vendedor.getId().equals(usuario.getId());
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPreco() { return preco; }
    public int getEstoque() { return estoque; }
    public Condicao getCondicao() { return condicao; }
    public Categoria getCategoria() { return categoria; }
    public Usuario getVendedor() { return vendedor; }
    public String getImagem() { return imagem; }
    public boolean isFreteGratis() { return freteGratis; }
    public int getVendidos() { return vendidos; }
    public double getNotaMedia() { return notaMedia; }
    public int getTotalAvaliacoes() { return totalAvaliacoes; }
    public boolean isAtivo() { return ativo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public void setImagem(String imagem) { this.imagem = imagem; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    void setVendidos(int vendidos) { this.vendidos = vendidos; }
}
