package com.pudimkail.vendeai.pedido;

import com.pudimkail.vendeai.carrinho.CarrinhoService;
import com.pudimkail.vendeai.carrinho.ItemCarrinho;
import com.pudimkail.vendeai.notificacao.NotificacaoService;
import com.pudimkail.vendeai.produto.NaoEncontradoException;
import com.pudimkail.vendeai.produto.Produto;
import com.pudimkail.vendeai.produto.ProdutoRepository;
import com.pudimkail.vendeai.usuario.Usuario;
import com.pudimkail.vendeai.usuario.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Fluxo da compra:
 * carrinho -> pedido (aguardando pagamento) -> pago -> enviado (cada vendedor envia os seus itens)
 * -> entregue (o comprador confirma) -> avaliação.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;
    private final UsuarioRepository usuarios;
    private final CarrinhoService carrinho;
    private final PixService pix;
    private final NotificacaoService notificacoes;

    public PedidoService(PedidoRepository pedidos, ProdutoRepository produtos, UsuarioRepository usuarios,
                         CarrinhoService carrinho, PixService pix, NotificacaoService notificacoes) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.usuarios = usuarios;
        this.carrinho = carrinho;
        this.pix = pix;
        this.notificacoes = notificacoes;
    }

    /** Fecha o carrinho em um pedido aguardando pagamento. O estoque só baixa quando o pagamento é aprovado. */
    @Transactional
    public Pedido criar(Usuario comprador, Endereco endereco, Pedido.FormaPagamento forma) {
        CarrinhoService.Resumo resumo = carrinho.resumo(comprador);
        if (resumo.itens().isEmpty()) {
            throw new IllegalArgumentException("Seu carrinho está vazio");
        }
        Pedido pedido = new Pedido(comprador, endereco, forma, resumo.subtotal(), resumo.frete());
        for (ItemCarrinho item : resumo.itens()) {
            Produto p = item.getProduto();
            if (!p.isDisponivel() || item.getQuantidade() > p.getEstoque()) {
                throw new IllegalArgumentException("\"" + p.getTitulo() + "\" não tem estoque suficiente");
            }
            pedido.adicionarItem(new Pedido.Item(p.getId(), p.getTitulo(), p.getPreco(), item.getQuantidade(),
                    p.getVendedor().getId(), p.getVendedor().getNome()));
        }
        pedidos.save(pedido);
        if (forma == Pedido.FormaPagamento.PIX) {
            pedido.setCodigoPix(pix.gerarCodigo(pedido.getId(), pedido.getTotal()));
        }
        carrinho.esvaziar(comprador);
        return pedido;
    }

    public List<Pedido> doComprador(Usuario comprador) {
        return pedidos.findByCompradorOrderByCriadoEmDesc(comprador);
    }

    public Pedido doComprador(Usuario comprador, Long id) {
        return pedidos.findByIdAndComprador(id, comprador).orElseThrow(() -> new NaoEncontradoException("Pedido não encontrado"));
    }

    public List<Pedido> vendas(Usuario vendedor) {
        return pedidos.findVendasDoVendedor(vendedor.getId());
    }

    @Transactional
    public Pedido pagarComCartao(Usuario comprador, Long id, String numero, String validade, String cvv, int parcelas) {
        Pedido pedido = aguardandoPagamento(comprador, id);
        String numeroLimpo = numero.replaceAll("\\D", "");
        String recusa = CartaoDemo.validar(numeroLimpo, validade.trim(), cvv.trim());
        if (recusa != null) {
            throw new PagamentoRecusadoException(recusa);
        }
        aprovar(pedido, numeroLimpo.substring(numeroLimpo.length() - 4), Math.min(Math.max(parcelas, 1), 10));
        return pedido;
    }

    /** No modo demonstração, o botão "Já paguei" simula a confirmação do banco. */
    @Transactional
    public Pedido confirmarPix(Usuario comprador, Long id) {
        Pedido pedido = aguardandoPagamento(comprador, id);
        aprovar(pedido, null, 1);
        return pedido;
    }

    @Transactional
    public Pedido cancelar(Usuario comprador, Long id) {
        Pedido pedido = aguardandoPagamento(comprador, id);
        pedido.setStatus(Pedido.Status.CANCELADO);
        return pedido;
    }

    /** O vendedor marca os itens dele como enviados. Quando todos os vendedores enviam, o pedido fica "Enviado". */
    @Transactional
    public Pedido marcarEnviado(Usuario vendedor, Long id) {
        Pedido pedido = pedidos.findById(id).orElseThrow(() -> new NaoEncontradoException("Venda não encontrada"));
        List<Pedido.Item> meus = pedido.itensDoVendedor(vendedor.getId());
        if (meus.isEmpty() || pedido.getStatus() != Pedido.Status.PAGO) {
            throw new IllegalArgumentException("Essa venda não pode ser marcada como enviada");
        }
        meus.forEach(Pedido.Item::marcarEnviado);
        if (pedido.getItens().stream().allMatch(Pedido.Item::isEnviado)) {
            pedido.setStatus(Pedido.Status.ENVIADO);
        }
        notificacoes.notificar(pedido.getComprador(), "🚚",
                vendedor.getNome() + " enviou: " + titulos(meus), "/compras/" + pedido.getId());
        return pedido;
    }

    @Transactional
    public Pedido confirmarRecebimento(Usuario comprador, Long id) {
        Pedido pedido = doComprador(comprador, id);
        if (pedido.getStatus() != Pedido.Status.ENVIADO) {
            throw new IllegalArgumentException("O pedido ainda não foi enviado");
        }
        pedido.setStatus(Pedido.Status.ENTREGUE);
        notificacoes.notificar(comprador, "⭐", "Seu pedido #" + id + " chegou! Conte o que achou avaliando os produtos.",
                "/compras/" + id);
        vendedoresDo(pedido).forEach(v -> notificacoes.notificar(v, "📦",
                comprador.getPrimeiroNome() + " confirmou que recebeu o pedido #" + id, "/vendas"));
        return pedido;
    }

    private Pedido aguardandoPagamento(Usuario comprador, Long id) {
        Pedido pedido = doComprador(comprador, id);
        if (!pedido.isAguardandoPagamento()) {
            throw new IllegalArgumentException("Este pedido não está aguardando pagamento");
        }
        return pedido;
    }

    /** Pagamento aprovado: baixa o estoque e avisa comprador e vendedores. */
    private void aprovar(Pedido pedido, String cartaoFinal, int parcelas) {
        for (Pedido.Item item : pedido.getItens()) {
            Produto produto = produtos.findById(item.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não existe mais: " + item.getTitulo()));
            try {
                produto.vender(item.getQuantidade());
            } catch (IllegalStateException e) {
                throw new PagamentoRecusadoException("\"" + item.getTitulo() + "\" esgotou antes do pagamento");
            }
        }
        pedido.marcarPago(cartaoFinal, parcelas);

        notificacoes.notificar(pedido.getComprador(), "✅",
                "Pagamento do pedido #" + pedido.getId() + " aprovado! Agora é só aguardar o envio.", "/compras/" + pedido.getId());
        for (Usuario vendedor : vendedoresDo(pedido)) {
            List<Pedido.Item> itens = pedido.itensDoVendedor(vendedor.getId());
            notificacoes.notificar(vendedor, "💰", "Você vendeu: " + titulos(itens) + ". Prepare o envio!", "/vendas");
        }
    }

    private List<Usuario> vendedoresDo(Pedido pedido) {
        return pedido.getItens().stream().map(Pedido.Item::getVendedorId).distinct()
                .map(usuarios::findById).flatMap(java.util.Optional::stream).filter(Objects::nonNull).toList();
    }

    private static String titulos(List<Pedido.Item> itens) {
        return itens.stream().map(i -> i.getQuantidade() + "x " + i.getTitulo()).reduce((a, b) -> a + ", " + b).orElse("");
    }
}
