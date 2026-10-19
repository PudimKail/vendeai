package com.pudimkail.vendeai.avaliacao;

import com.pudimkail.vendeai.notificacao.NotificacaoService;
import com.pudimkail.vendeai.pedido.PedidoRepository;
import com.pudimkail.vendeai.produto.Produto;
import com.pudimkail.vendeai.produto.ProdutoService;
import com.pudimkail.vendeai.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Só quem comprou e recebeu o produto pode avaliar, uma vez por produto. */
@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacoes;
    private final PedidoRepository pedidos;
    private final ProdutoService produtos;
    private final NotificacaoService notificacoes;

    public AvaliacaoService(AvaliacaoRepository avaliacoes, PedidoRepository pedidos, ProdutoService produtos,
                            NotificacaoService notificacoes) {
        this.avaliacoes = avaliacoes;
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.notificacoes = notificacoes;
    }

    public List<Avaliacao> doProduto(Produto produto) {
        return avaliacoes.findByProdutoOrderByCriadoEmDesc(produto);
    }

    public boolean podeAvaliar(Usuario usuario, Produto produto) {
        return usuario != null && pedidos.recebeuProduto(usuario, produto.getId())
                && !avaliacoes.existsByProdutoAndAutor(produto, usuario);
    }

    public List<Long> produtosJaAvaliados(Usuario usuario) {
        return avaliacoes.findByAutor(usuario).stream().map(a -> a.getProduto().getId()).toList();
    }

    @Transactional
    public Avaliacao avaliar(Usuario autor, Long produtoId, int nota, String comentario) {
        if (nota < 1 || nota > 5) {
            throw new IllegalArgumentException("A nota vai de 1 a 5 estrelas");
        }
        Produto produto = produtos.buscarPorId(produtoId);
        if (!podeAvaliar(autor, produto)) {
            throw new IllegalArgumentException("Você só pode avaliar produtos que comprou e recebeu, uma vez cada");
        }
        Avaliacao avaliacao = avaliacoes.save(new Avaliacao(produto, autor, nota, comentario));
        produto.registrarAvaliacao(nota);
        notificacoes.notificar(produto.getVendedor(), "⭐",
                autor.getPrimeiroNome() + " deu " + nota + " estrela(s) para \"" + produto.getTitulo() + "\"",
                "/produto/" + produtoId);
        return avaliacao;
    }
}
