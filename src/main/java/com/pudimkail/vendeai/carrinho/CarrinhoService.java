package com.pudimkail.vendeai.carrinho;

import com.pudimkail.vendeai.produto.NaoEncontradoException;
import com.pudimkail.vendeai.produto.Produto;
import com.pudimkail.vendeai.produto.ProdutoRepository;
import com.pudimkail.vendeai.usuario.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CarrinhoService {

    private final ItemCarrinhoRepository itens;
    private final ProdutoRepository produtos;
    private final BigDecimal valorFrete;
    private final BigDecimal freteGratisAcimaDe;

    public CarrinhoService(ItemCarrinhoRepository itens, ProdutoRepository produtos,
                           @Value("${vendeai.frete.valor}") BigDecimal valorFrete,
                           @Value("${vendeai.frete.gratis-acima-de}") BigDecimal freteGratisAcimaDe) {
        this.itens = itens;
        this.produtos = produtos;
        this.valorFrete = valorFrete;
        this.freteGratisAcimaDe = freteGratisAcimaDe;
    }

    public record Resumo(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal frete, BigDecimal total,
                         BigDecimal faltaParaFreteGratis) {
    }

    /** Frete grátis se todos os itens têm frete grátis ou se a compra passa do valor mínimo. */
    public Resumo resumo(Usuario usuario) {
        List<ItemCarrinho> lista = itens.findByUsuarioOrderById(usuario);
        BigDecimal subtotal = lista.stream().map(ItemCarrinho::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean todosGratis = lista.stream().allMatch(i -> i.getProduto().isFreteGratis());
        boolean gratis = lista.isEmpty() || todosGratis || subtotal.compareTo(freteGratisAcimaDe) >= 0;
        BigDecimal frete = gratis ? BigDecimal.ZERO : valorFrete;
        BigDecimal falta = gratis ? BigDecimal.ZERO : freteGratisAcimaDe.subtract(subtotal);
        return new Resumo(lista, subtotal, frete, subtotal.add(frete), falta);
    }

    public int quantidade(Usuario usuario) {
        return itens.contarItens(usuario);
    }

    @Transactional
    public void adicionar(Usuario usuario, Long produtoId, int quantidade) {
        Produto produto = produtos.findById(produtoId).orElseThrow(() -> new NaoEncontradoException("Produto não encontrado"));
        if (produto.pertenceA(usuario)) {
            throw new IllegalArgumentException("Você não pode comprar o seu próprio anúncio");
        }
        if (!produto.isDisponivel()) {
            throw new IllegalArgumentException("Este produto não está disponível");
        }
        ItemCarrinho item = itens.findByUsuarioAndProduto(usuario, produto)
                .orElseGet(() -> new ItemCarrinho(usuario, produto, 0));
        int nova = item.getQuantidade() + Math.max(quantidade, 1);
        if (nova > produto.getEstoque()) {
            throw new IllegalArgumentException("Só temos " + produto.getEstoque() + " unidade(s) em estoque");
        }
        item.setQuantidade(nova);
        itens.save(item);
    }

    @Transactional
    public void alterarQuantidade(Usuario usuario, Long itemId, int quantidade) {
        ItemCarrinho item = itens.findByIdAndUsuario(itemId, usuario)
                .orElseThrow(() -> new NaoEncontradoException("Item não encontrado"));
        if (quantidade <= 0) {
            itens.delete(item);
            return;
        }
        if (quantidade > item.getProduto().getEstoque()) {
            throw new IllegalArgumentException("Só temos " + item.getProduto().getEstoque() + " unidade(s) em estoque");
        }
        item.setQuantidade(quantidade);
    }

    @Transactional
    public void remover(Usuario usuario, Long itemId) {
        itens.findByIdAndUsuario(itemId, usuario).ifPresent(itens::delete);
    }

    @Transactional
    public void esvaziar(Usuario usuario) {
        itens.deleteByUsuario(usuario);
    }
}
