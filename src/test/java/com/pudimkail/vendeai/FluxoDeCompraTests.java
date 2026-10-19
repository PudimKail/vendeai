package com.pudimkail.vendeai;

import com.pudimkail.vendeai.avaliacao.AvaliacaoService;
import com.pudimkail.vendeai.carrinho.CarrinhoService;
import com.pudimkail.vendeai.notificacao.NotificacaoService;
import com.pudimkail.vendeai.pedido.Endereco;
import com.pudimkail.vendeai.pedido.PagamentoRecusadoException;
import com.pudimkail.vendeai.pedido.Pedido;
import com.pudimkail.vendeai.pedido.PedidoService;
import com.pudimkail.vendeai.produto.Filtros;
import com.pudimkail.vendeai.produto.Produto;
import com.pudimkail.vendeai.produto.ProdutoRepository;
import com.pudimkail.vendeai.produto.ProdutoService;
import com.pudimkail.vendeai.usuario.CadastroForm;
import com.pudimkail.vendeai.usuario.Usuario;
import com.pudimkail.vendeai.usuario.UsuarioRepository;
import com.pudimkail.vendeai.usuario.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class FluxoDeCompraTests {

    @Autowired UsuarioService usuarioService;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProdutoService produtoService;
    @Autowired ProdutoRepository produtos;
    @Autowired CarrinhoService carrinho;
    @Autowired PedidoService pedidos;
    @Autowired AvaliacaoService avaliacoes;
    @Autowired NotificacaoService notificacoes;

    @Test
    void cadastroComCodigoDeVerificacao() {
        CadastroForm form = new CadastroForm();
        form.setNome("Maria Teste");
        form.setEmail("Maria@Teste.com");
        form.setSenha("segredo1");
        form.setConfirmacaoSenha("segredo1");

        UsuarioService.Cadastro cadastro = usuarioService.cadastrar(form);
        Usuario maria = cadastro.usuario();
        assertThat(maria.getEmail()).isEqualTo("maria@teste.com");
        assertThat(maria.isEmailVerificado()).isFalse();
        assertThat(cadastro.codigoDemo()).matches("\\d{6}");

        // Sem confirmar o e-mail, não dá para comprar
        assertThatThrownBy(() -> usuarioService.exigirVerificado(() -> "maria@teste.com"))
                .hasMessageContaining("Confirme seu e-mail");

        String errado = cadastro.codigoDemo().equals("000000") ? "111111" : "000000";
        assertThatThrownBy(() -> usuarioService.verificarEmail(maria, errado)).hasMessageContaining("4 tentativa");
        assertThatThrownBy(() -> usuarioService.verificarEmail(maria, errado)).hasMessageContaining("3 tentativa");

        usuarioService.verificarEmail(maria, cadastro.codigoDemo());
        assertThat(usuarios.findByEmail("maria@teste.com").orElseThrow().isEmailVerificado()).isTrue();

        // E-mail repetido não pode
        assertThatThrownBy(() -> usuarioService.cadastrar(form)).hasMessageContaining("Já existe uma conta");
    }

    @Test
    void codigoBloqueiaDepoisDeCincoErros() {
        CadastroForm form = new CadastroForm();
        form.setNome("Joao");
        form.setEmail("joao@teste.com");
        form.setSenha("segredo1");
        form.setConfirmacaoSenha("segredo1");
        UsuarioService.Cadastro cadastro = usuarioService.cadastrar(form);
        String errado = cadastro.codigoDemo().equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> usuarioService.verificarEmail(cadastro.usuario(), errado));
        }
        // Mesmo o código certo não vale mais: precisa pedir outro
        assertThatThrownBy(() -> usuarioService.verificarEmail(cadastro.usuario(), cadastro.codigoDemo()))
                .hasMessageContaining("Muitas tentativas");
    }

    @Test
    void compraCompletaComDoisVendedores() {
        Usuario comprador = usuarios.findByEmail("comprador@vendeai.dev").orElseThrow();
        Usuario tech = usuarios.findByEmail("techstore@vendeai.dev").orElseThrow();
        Usuario loja = usuarios.findByEmail("loja@vendeai.dev").orElseThrow();
        Produto monitor = produtoDe(tech, "Monitor Gamer");
        Produto mouse = produtoDe(loja, "Mouse sem Fio");
        int estoqueMonitor = monitor.getEstoque();

        carrinho.adicionar(comprador, monitor.getId(), 1);
        carrinho.adicionar(comprador, mouse.getId(), 2);
        assertThat(carrinho.quantidade(comprador)).isEqualTo(3);

        Pedido pedido = pedidos.criar(comprador, endereco(), Pedido.FormaPagamento.PIX);
        assertThat(pedido.getTotal()).isEqualByComparingTo(new BigDecimal("969.70")); // 849,90 + 2x59,90, frete grátis
        assertThat(pedido.getCodigoPix()).startsWith("000201").contains("br.gov.bcb.pix");
        assertThat(carrinho.quantidade(comprador)).isZero();

        pedidos.confirmarPix(comprador, pedido.getId());
        assertThat(produtos.findById(monitor.getId()).orElseThrow().getEstoque()).isEqualTo(estoqueMonitor - 1);
        assertThat(notificacoes.listar(tech)).anyMatch(n -> n.getMensagem().contains("Você vendeu: 1x Monitor"));

        // Cada vendedor envia a sua parte
        assertThat(pedidos.marcarEnviado(tech, pedido.getId()).getStatus()).isEqualTo(Pedido.Status.PAGO);
        assertThat(pedidos.marcarEnviado(loja, pedido.getId()).getStatus()).isEqualTo(Pedido.Status.ENVIADO);
        assertThat(notificacoes.listar(comprador)).anyMatch(n -> n.getMensagem().contains("enviou"));

        // Só avalia depois de receber
        assertThat(avaliacoes.podeAvaliar(comprador, monitor)).isFalse();
        pedidos.confirmarRecebimento(comprador, pedido.getId());
        assertThat(avaliacoes.podeAvaliar(comprador, monitor)).isTrue();

        avaliacoes.avaliar(comprador, monitor.getId(), 4, "Ótimo monitor");
        Produto avaliado = produtos.findById(monitor.getId()).orElseThrow();
        assertThat(avaliado.getTotalAvaliacoes()).isEqualTo(1);
        assertThat(avaliado.getNotaMedia()).isEqualTo(4.0);
        assertThatThrownBy(() -> avaliacoes.avaliar(comprador, monitor.getId(), 5, "de novo"))
                .hasMessageContaining("uma vez");
        assertThat(pedidos.vendas(tech)).anyMatch(p -> p.getId().equals(pedido.getId()));
    }

    @Test
    void cartaoRecusadoNaoBaixaEstoque() {
        Usuario comprador = usuarios.findByEmail("comprador@vendeai.dev").orElseThrow();
        Produto furadeira = produtoDe(usuarios.findByEmail("loja@vendeai.dev").orElseThrow(), "Furadeira");
        int estoque = furadeira.getEstoque();
        carrinho.adicionar(comprador, furadeira.getId(), 1);
        Pedido pedido = pedidos.criar(comprador, endereco(), Pedido.FormaPagamento.CARTAO);

        assertThatThrownBy(() -> pedidos.pagarComCartao(comprador, pedido.getId(), "4000 0000 0000 0002", "12/30", "123", 1))
                .isInstanceOf(PagamentoRecusadoException.class).hasMessageContaining("recusado");
        assertThatThrownBy(() -> pedidos.pagarComCartao(comprador, pedido.getId(), "4111 1111 1111 1112", "12/30", "123", 1))
                .hasMessageContaining("inválido");
        assertThat(produtos.findById(furadeira.getId()).orElseThrow().getEstoque()).isEqualTo(estoque);

        Pedido pago = pedidos.pagarComCartao(comprador, pedido.getId(), "4111 1111 1111 1111", "12/30", "123", 3);
        assertThat(pago.getStatus()).isEqualTo(Pedido.Status.PAGO);
        assertThat(pago.getCartaoFinal()).isEqualTo("1111");
        assertThat(pago.getParcelas()).isEqualTo(3);
    }

    @Test
    void naoCompraOProprioAnuncioNemMaisQueOEstoque() {
        Usuario tech = usuarios.findByEmail("techstore@vendeai.dev").orElseThrow();
        Usuario comprador = usuarios.findByEmail("comprador@vendeai.dev").orElseThrow();
        Produto ps5 = produtoDe(tech, "PlayStation 5");
        assertThatThrownBy(() -> carrinho.adicionar(tech, ps5.getId(), 1)).hasMessageContaining("próprio anúncio");
        assertThatThrownBy(() -> carrinho.adicionar(comprador, ps5.getId(), 999)).hasMessageContaining("estoque");
    }

    @Test
    void buscaComFiltrosEOrdenacao() {
        var notebooks = produtoService.buscar(new Filtros("notebook", null, null, null, null, false, null), 0);
        // 2 notebooks + a mochila "para Notebook"
        assertThat(notebooks.getContent()).hasSize(3).allMatch(p -> p.getTitulo().contains("Notebook"));

        var usados = produtoService.buscar(new Filtros("notebook", null, null, null, "USADO", false, null), 0);
        assertThat(usados.getContent()).hasSize(1);

        var baratos = produtoService.buscar(new Filtros(null, "informatica", null, new BigDecimal("300"), null, false,
                Filtros.MENOR_PRECO), 0);
        assertThat(baratos.getContent()).extracting(Produto::getTitulo)
                .containsExactly("Mouse sem Fio Ergonômico", "Teclado Mecânico RGB Switch Blue");
    }

    private Produto produtoDe(Usuario vendedor, String trechoDoTitulo) {
        return produtoService.doVendedor(vendedor).stream()
                .filter(p -> p.getTitulo().contains(trechoDoTitulo)).findFirst().orElseThrow();
    }

    private static Endereco endereco() {
        Endereco e = new Endereco();
        e.setCep("78360-000");
        e.setRua("Rua das Palmeiras");
        e.setNumero("120");
        e.setBairro("Centro");
        e.setCidade("Campo Novo do Parecis");
        e.setUf("MT");
        return e;
    }
}
