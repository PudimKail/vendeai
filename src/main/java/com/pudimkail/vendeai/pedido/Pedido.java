package com.pudimkail.vendeai.pedido;

import com.pudimkail.vendeai.usuario.Usuario;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {

    public enum Status {
        AGUARDANDO_PAGAMENTO("Aguardando pagamento"),
        PAGO("Pagamento aprovado"),
        ENVIADO("Enviado"),
        ENTREGUE("Entregue"),
        CANCELADO("Cancelado");

        private final String rotulo;

        Status(String rotulo) { this.rotulo = rotulo; }

        public String getRotulo() { return rotulo; }
    }

    public enum FormaPagamento {
        PIX("Pix"), CARTAO("Cartão de crédito");

        private final String rotulo;

        FormaPagamento(String rotulo) { this.rotulo = rotulo; }

        public String getRotulo() { return rotulo; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario comprador;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "pedido_itens", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderColumn(name = "posicao")
    private List<Item> itens = new ArrayList<>();

    @Embedded
    private Endereco endereco;

    @Enumerated(EnumType.STRING)
    private Status status = Status.AGUARDANDO_PAGAMENTO;

    @Enumerated(EnumType.STRING)
    private FormaPagamento formaPagamento;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal frete;

    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    /** Código "copia e cola" do Pix (modo demonstração). */
    @Column(length = 512)
    private String codigoPix;

    /** Só os 4 últimos dígitos: o número completo do cartão nunca é salvo. */
    private String cartaoFinal;
    private int parcelas = 1;

    private LocalDateTime criadoEm = LocalDateTime.now();
    private LocalDateTime pagoEm;

    protected Pedido() {
    }

    public Pedido(Usuario comprador, Endereco endereco, FormaPagamento formaPagamento,
                  BigDecimal subtotal, BigDecimal frete) {
        this.comprador = comprador;
        this.endereco = endereco;
        this.formaPagamento = formaPagamento;
        this.subtotal = subtotal;
        this.frete = frete;
        this.total = subtotal.add(frete);
    }

    public void adicionarItem(Item item) {
        itens.add(item);
    }

    void marcarPago(String cartaoFinal, int parcelas) {
        this.status = Status.PAGO;
        this.pagoEm = LocalDateTime.now();
        this.cartaoFinal = cartaoFinal;
        this.parcelas = parcelas;
    }

    void setCodigoPix(String codigoPix) { this.codigoPix = codigoPix; }

    void setStatus(Status status) { this.status = status; }

    public boolean isAguardandoPagamento() {
        return status == Status.AGUARDANDO_PAGAMENTO;
    }

    public boolean isEntregue() {
        return status == Status.ENTREGUE;
    }

    /** Status visto pelo vendedor: os itens dele já foram enviados? */
    public boolean itensEnviadosPor(Long vendedorId) {
        return itens.stream().filter(i -> i.getVendedorId().equals(vendedorId)).allMatch(Item::isEnviado);
    }

    public List<Item> itensDoVendedor(Long vendedorId) {
        return itens.stream().filter(i -> i.getVendedorId().equals(vendedorId)).toList();
    }

    public BigDecimal totalDoVendedor(Long vendedorId) {
        return itensDoVendedor(vendedorId).stream().map(Item::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getQuantidadeItens() {
        return itens.stream().mapToInt(Item::getQuantidade).sum();
    }

    public Long getId() { return id; }
    public Usuario getComprador() { return comprador; }
    public List<Item> getItens() { return itens; }
    public Endereco getEndereco() { return endereco; }
    public Status getStatus() { return status; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getFrete() { return frete; }
    public BigDecimal getTotal() { return total; }
    public String getCodigoPix() { return codigoPix; }
    public String getCartaoFinal() { return cartaoFinal; }
    public int getParcelas() { return parcelas; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getPagoEm() { return pagoEm; }

    /** Foto do produto no momento da compra: preço e título não mudam se o anúncio for editado depois. */
    @Embeddable
    public static class Item {
        private Long produtoId;
        private String titulo;
        @Column(precision = 10, scale = 2)
        private BigDecimal precoUnitario;
        private int quantidade;
        private Long vendedorId;
        private String vendedorNome;
        /** Cada vendedor marca o envio dos seus itens. */
        private boolean enviado;

        protected Item() {
        }

        public boolean isEnviado() { return enviado; }

        void marcarEnviado() { this.enviado = true; }

        public Item(Long produtoId, String titulo, BigDecimal precoUnitario, int quantidade, Long vendedorId, String vendedorNome) {
            this.produtoId = produtoId;
            this.titulo = titulo;
            this.precoUnitario = precoUnitario;
            this.quantidade = quantidade;
            this.vendedorId = vendedorId;
            this.vendedorNome = vendedorNome;
        }

        public BigDecimal getSubtotal() {
            return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
        }

        public Long getProdutoId() { return produtoId; }
        public String getTitulo() { return titulo; }
        public BigDecimal getPrecoUnitario() { return precoUnitario; }
        public int getQuantidade() { return quantidade; }
        public Long getVendedorId() { return vendedorId; }
        public String getVendedorNome() { return vendedorNome; }
    }
}
