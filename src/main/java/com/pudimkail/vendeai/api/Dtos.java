package com.pudimkail.vendeai.api;

import com.pudimkail.vendeai.avaliacao.Avaliacao;
import com.pudimkail.vendeai.carrinho.CarrinhoService;
import com.pudimkail.vendeai.carrinho.ItemCarrinho;
import com.pudimkail.vendeai.notificacao.Notificacao;
import com.pudimkail.vendeai.pedido.Endereco;
import com.pudimkail.vendeai.pedido.Pedido;
import com.pudimkail.vendeai.produto.Categoria;
import com.pudimkail.vendeai.produto.Produto;
import com.pudimkail.vendeai.usuario.Usuario;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato JSON que a API devolve para o React.
 * As entidades nunca vão direto para a resposta (o hash da senha, por exemplo, nunca sai do servidor).
 */
public final class Dtos {

    private Dtos() {
    }

    public record UsuarioDto(Long id, String nome, String primeiroNome, String email, boolean emailVerificado) {
        public static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getNome(), u.getPrimeiroNome(), u.getEmail(), u.isEmailVerificado());
        }
    }

    /**
     * Quem está logado + contadores do cabeçalho (carrinho e sininho).
     * codigoDemo só vem preenchido no modo demonstração de e-mail, logo após o cadastro ou reenvio.
     */
    public record SessaoDto(UsuarioDto usuario, int itensCarrinho, long notificacoesNaoLidas, String codigoDemo) {
        public static SessaoDto anonima() {
            return new SessaoDto(null, 0, 0, null);
        }
    }

    public record CodigoRequest(String codigo) {
    }

    public record CategoriaDto(Long id, String nome, String slug, String icone) {
        public static CategoriaDto de(Categoria c) {
            return new CategoriaDto(c.getId(), c.getNome(), c.getSlug(), c.getIcone());
        }
    }

    public record ProdutoDto(Long id, String titulo, String descricao, BigDecimal preco, BigDecimal valorParcela,
                             int parcelas, int estoque, String condicao, String condicaoRotulo, CategoriaDto categoria,
                             UsuarioResumo vendedor, String imagemUrl, boolean freteGratis, int vendidos,
                             double notaMedia, int totalAvaliacoes, boolean ativo, LocalDateTime criadoEm) {
        public static ProdutoDto de(Produto p) {
            return new ProdutoDto(p.getId(), p.getTitulo(), p.getDescricao(), p.getPreco(), p.getValorParcela(),
                    Produto.PARCELAS_SEM_JUROS, p.getEstoque(), p.getCondicao().name(), p.getCondicao().getRotulo(),
                    CategoriaDto.de(p.getCategoria()), UsuarioResumo.de(p.getVendedor()),
                    p.getImagem() == null ? null : "/uploads/" + p.getImagem(), p.isFreteGratis(), p.getVendidos(),
                    p.getNotaMedia(), p.getTotalAvaliacoes(), p.isAtivo(), p.getCriadoEm());
        }

        public static List<ProdutoDto> de(List<Produto> produtos) {
            return produtos.stream().map(ProdutoDto::de).toList();
        }
    }

    public record UsuarioResumo(Long id, String nome) {
        public static UsuarioResumo de(Usuario u) {
            return new UsuarioResumo(u.getId(), u.getNome());
        }
    }

    public record PaginaDto<T>(List<T> itens, int pagina, int totalPaginas, long totalItens) {
        public static PaginaDto<ProdutoDto> de(Page<Produto> page) {
            return new PaginaDto<>(ProdutoDto.de(page.getContent()), page.getNumber(), page.getTotalPages(),
                    page.getTotalElements());
        }
    }

    public record AvaliacaoDto(Long id, String autor, int nota, String comentario, LocalDateTime criadoEm) {
        public static AvaliacaoDto de(Avaliacao a) {
            return new AvaliacaoDto(a.getId(), a.getAutor().getPrimeiroNome(), a.getNota(), a.getComentario(), a.getCriadoEm());
        }
    }

    /** Página do produto: o produto, semelhantes, avaliações e o que o usuário logado pode fazer. */
    public record ProdutoDetalheDto(ProdutoDto produto, List<ProdutoDto> semelhantes, List<AvaliacaoDto> avaliacoes,
                                    boolean podeAvaliar, boolean meuAnuncio) {
    }

    public record ItemCarrinhoDto(Long id, ProdutoDto produto, int quantidade, BigDecimal subtotal) {
        public static ItemCarrinhoDto de(ItemCarrinho i) {
            return new ItemCarrinhoDto(i.getId(), ProdutoDto.de(i.getProduto()), i.getQuantidade(), i.getSubtotal());
        }
    }

    public record CarrinhoDto(List<ItemCarrinhoDto> itens, BigDecimal subtotal, BigDecimal frete, BigDecimal total,
                              BigDecimal faltaParaFreteGratis) {
        public static CarrinhoDto de(CarrinhoService.Resumo r) {
            return new CarrinhoDto(r.itens().stream().map(ItemCarrinhoDto::de).toList(), r.subtotal(), r.frete(),
                    r.total(), r.faltaParaFreteGratis());
        }
    }

    public record ItemPedidoDto(Long produtoId, String titulo, BigDecimal precoUnitario, int quantidade,
                                BigDecimal subtotal, String vendedor, boolean enviado, boolean jaAvaliado) {
        static ItemPedidoDto de(Pedido.Item i, List<Long> avaliados) {
            return new ItemPedidoDto(i.getProdutoId(), i.getTitulo(), i.getPrecoUnitario(), i.getQuantidade(),
                    i.getSubtotal(), i.getVendedorNome(), i.isEnviado(), avaliados.contains(i.getProdutoId()));
        }
    }

    public record PedidoDto(Long id, String status, String statusRotulo, String formaPagamento, String formaPagamentoRotulo,
                            List<ItemPedidoDto> itens, Endereco endereco, BigDecimal subtotal, BigDecimal frete,
                            BigDecimal total, String codigoPix, String pixQrCode, String cartaoFinal, int parcelas,
                            LocalDateTime criadoEm, LocalDateTime pagoEm) {
        public static PedidoDto de(Pedido p, List<Long> avaliados, String pixQrCode) {
            return new PedidoDto(p.getId(), p.getStatus().name(), p.getStatus().getRotulo(), p.getFormaPagamento().name(),
                    p.getFormaPagamento().getRotulo(), p.getItens().stream().map(i -> ItemPedidoDto.de(i, avaliados)).toList(),
                    p.getEndereco(), p.getSubtotal(), p.getFrete(), p.getTotal(), p.getCodigoPix(), pixQrCode,
                    p.getCartaoFinal(), p.getParcelas(), p.getCriadoEm(), p.getPagoEm());
        }
    }

    /** Uma venda vista pelo vendedor: só os itens dele. */
    public record VendaDto(Long pedidoId, String comprador, String status, String statusRotulo, String endereco,
                           List<ItemPedidoDto> itens, BigDecimal total, boolean enviado, LocalDateTime pagoEm) {
        public static VendaDto de(Pedido p, Long vendedorId) {
            return new VendaDto(p.getId(), p.getComprador().getNome(), p.getStatus().name(), p.getStatus().getRotulo(),
                    p.getEndereco().getResumo(),
                    p.itensDoVendedor(vendedorId).stream().map(i -> ItemPedidoDto.de(i, List.of())).toList(),
                    p.totalDoVendedor(vendedorId), p.itensEnviadosPor(vendedorId), p.getPagoEm());
        }
    }

    public record NotificacaoDto(Long id, String icone, String mensagem, String link, boolean lida, LocalDateTime criadoEm) {
        public static NotificacaoDto de(Notificacao n) {
            return new NotificacaoDto(n.getId(), n.getIcone(), n.getMensagem(), n.getLink(), n.isLida(), n.getCriadoEm());
        }
    }

    // ---- Corpos das requisições ----

    public record LoginRequest(String email, String senha) {
    }

    public record AdicionarCarrinhoRequest(Long produtoId, int quantidade) {
    }

    public record QuantidadeRequest(int quantidade) {
    }

    public record CheckoutRequest(@jakarta.validation.Valid Endereco endereco, Pedido.FormaPagamento formaPagamento) {
    }

    public record CartaoRequest(String numero, String nome, String validade, String cvv, int parcelas) {
    }

    public record AvaliacaoRequest(int nota, String comentario) {
    }
}
