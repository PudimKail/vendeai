package com.pudimkail.vendeai.api;

import com.pudimkail.vendeai.api.Dtos.AvaliacaoDto;
import com.pudimkail.vendeai.api.Dtos.AvaliacaoRequest;
import com.pudimkail.vendeai.api.Dtos.CategoriaDto;
import com.pudimkail.vendeai.api.Dtos.PaginaDto;
import com.pudimkail.vendeai.api.Dtos.ProdutoDetalheDto;
import com.pudimkail.vendeai.api.Dtos.ProdutoDto;
import com.pudimkail.vendeai.avaliacao.AvaliacaoService;
import com.pudimkail.vendeai.produto.Filtros;
import com.pudimkail.vendeai.produto.Produto;
import com.pudimkail.vendeai.produto.ProdutoService;
import com.pudimkail.vendeai.usuario.Usuario;
import com.pudimkail.vendeai.usuario.UsuarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/** Vitrine pública: categorias, busca e página do produto. */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final ProdutoService produtos;
    private final AvaliacaoService avaliacoes;
    private final UsuarioService usuarios;

    public CatalogoController(ProdutoService produtos, AvaliacaoService avaliacoes, UsuarioService usuarios) {
        this.produtos = produtos;
        this.avaliacoes = avaliacoes;
        this.usuarios = usuarios;
    }

    @GetMapping("/categorias")
    public List<CategoriaDto> categorias() {
        return produtos.categorias().stream().map(CategoriaDto::de).toList();
    }

    /** Ex.: /api/produtos?q=iphone&categoria=celulares&max=3000&ordem=menor-preco&pagina=0 */
    @GetMapping("/produtos")
    public PaginaDto<ProdutoDto> buscar(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) String categoria,
                                        @RequestParam(required = false) BigDecimal min,
                                        @RequestParam(required = false) BigDecimal max,
                                        @RequestParam(required = false) String condicao,
                                        @RequestParam(defaultValue = "false") boolean freteGratis,
                                        @RequestParam(required = false) String ordem,
                                        @RequestParam(defaultValue = "0") int pagina) {
        return PaginaDto.de(produtos.buscar(new Filtros(q, categoria, min, max, condicao, freteGratis, ordem), pagina));
    }

    @GetMapping("/produtos/destaques")
    public Map<String, List<ProdutoDto>> destaques() {
        return Map.of("maisVendidos", ProdutoDto.de(produtos.maisVendidos()),
                "recentes", ProdutoDto.de(produtos.recentes()));
    }

    @GetMapping("/produtos/{id}")
    public ProdutoDetalheDto produto(@PathVariable Long id, Principal principal) {
        Produto produto = produtos.buscarPorId(id);
        Usuario usuario = usuarios.atual(principal).orElse(null);
        return new ProdutoDetalheDto(ProdutoDto.de(produto), ProdutoDto.de(produtos.semelhantes(produto)),
                avaliacoes.doProduto(produto).stream().map(AvaliacaoDto::de).toList(),
                avaliacoes.podeAvaliar(usuario, produto), produto.pertenceA(usuario));
    }

    @PostMapping("/produtos/{id}/avaliacoes")
    public AvaliacaoDto avaliar(@PathVariable Long id, @RequestBody AvaliacaoRequest req, Principal principal) {
        return AvaliacaoDto.de(avaliacoes.avaliar(usuarios.exigir(principal), id, req.nota(), req.comentario()));
    }
}
