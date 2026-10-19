package com.pudimkail.vendeai.api;

import com.pudimkail.vendeai.api.Dtos.AdicionarCarrinhoRequest;
import com.pudimkail.vendeai.api.Dtos.CarrinhoDto;
import com.pudimkail.vendeai.api.Dtos.CartaoRequest;
import com.pudimkail.vendeai.api.Dtos.CheckoutRequest;
import com.pudimkail.vendeai.api.Dtos.PedidoDto;
import com.pudimkail.vendeai.api.Dtos.QuantidadeRequest;
import com.pudimkail.vendeai.avaliacao.AvaliacaoService;
import com.pudimkail.vendeai.carrinho.CarrinhoService;
import com.pudimkail.vendeai.pedido.Pedido;
import com.pudimkail.vendeai.pedido.PedidoService;
import com.pudimkail.vendeai.pedido.PixService;
import com.pudimkail.vendeai.usuario.Usuario;
import com.pudimkail.vendeai.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/** Carrinho, checkout, pagamento e "Minhas compras". */
@RestController
@RequestMapping("/api")
public class CompraController {

    private final UsuarioService usuarios;
    private final CarrinhoService carrinho;
    private final PedidoService pedidos;
    private final PixService pix;
    private final AvaliacaoService avaliacoes;

    public CompraController(UsuarioService usuarios, CarrinhoService carrinho, PedidoService pedidos, PixService pix,
                            AvaliacaoService avaliacoes) {
        this.usuarios = usuarios;
        this.carrinho = carrinho;
        this.pedidos = pedidos;
        this.pix = pix;
        this.avaliacoes = avaliacoes;
    }

    // ---- Carrinho ----

    @GetMapping("/carrinho")
    public CarrinhoDto carrinho(Principal principal) {
        return CarrinhoDto.de(carrinho.resumo(usuarios.exigir(principal)));
    }

    @PostMapping("/carrinho")
    public CarrinhoDto adicionar(@RequestBody AdicionarCarrinhoRequest req, Principal principal) {
        Usuario u = usuarios.exigirVerificado(principal);
        carrinho.adicionar(u, req.produtoId(), req.quantidade());
        return CarrinhoDto.de(carrinho.resumo(u));
    }

    @PutMapping("/carrinho/{itemId}")
    public CarrinhoDto alterar(@PathVariable Long itemId, @RequestBody QuantidadeRequest req, Principal principal) {
        Usuario u = usuarios.exigir(principal);
        carrinho.alterarQuantidade(u, itemId, req.quantidade());
        return CarrinhoDto.de(carrinho.resumo(u));
    }

    @DeleteMapping("/carrinho/{itemId}")
    public CarrinhoDto remover(@PathVariable Long itemId, Principal principal) {
        Usuario u = usuarios.exigir(principal);
        carrinho.remover(u, itemId);
        return CarrinhoDto.de(carrinho.resumo(u));
    }

    // ---- Pedidos (comprador) ----

    @PostMapping("/pedidos")
    public PedidoDto finalizar(@Valid @RequestBody CheckoutRequest req, Principal principal) {
        if (req.formaPagamento() == null) {
            throw new IllegalArgumentException("Escolha a forma de pagamento");
        }
        Usuario u = usuarios.exigirVerificado(principal);
        return dto(u, pedidos.criar(u, req.endereco(), req.formaPagamento()));
    }

    @GetMapping("/pedidos")
    public List<PedidoDto> minhasCompras(Principal principal) {
        Usuario u = usuarios.exigir(principal);
        List<Long> avaliados = avaliacoes.produtosJaAvaliados(u);
        return pedidos.doComprador(u).stream().map(p -> PedidoDto.de(p, avaliados, null)).toList();
    }

    @GetMapping("/pedidos/{id}")
    public PedidoDto pedido(@PathVariable Long id, Principal principal) {
        Usuario u = usuarios.exigir(principal);
        return dto(u, pedidos.doComprador(u, id));
    }

    @PostMapping("/pedidos/{id}/cartao")
    public PedidoDto pagarCartao(@PathVariable Long id, @RequestBody CartaoRequest req, Principal principal) {
        Usuario u = usuarios.exigir(principal);
        return dto(u, pedidos.pagarComCartao(u, id, req.numero(), req.validade(), req.cvv(), req.parcelas()));
    }

    @PostMapping("/pedidos/{id}/pix")
    public PedidoDto confirmarPix(@PathVariable Long id, Principal principal) {
        Usuario u = usuarios.exigir(principal);
        return dto(u, pedidos.confirmarPix(u, id));
    }

    @PostMapping("/pedidos/{id}/cancelar")
    public PedidoDto cancelar(@PathVariable Long id, Principal principal) {
        Usuario u = usuarios.exigir(principal);
        return dto(u, pedidos.cancelar(u, id));
    }

    @PostMapping("/pedidos/{id}/recebido")
    public PedidoDto recebido(@PathVariable Long id, Principal principal) {
        Usuario u = usuarios.exigir(principal);
        return dto(u, pedidos.confirmarRecebimento(u, id));
    }

    private PedidoDto dto(Usuario u, Pedido p) {
        String qr = p.isAguardandoPagamento() && p.getCodigoPix() != null ? pix.qrCodeDataUri(p.getCodigoPix()) : null;
        return PedidoDto.de(p, avaliacoes.produtosJaAvaliados(u), qr);
    }
}
