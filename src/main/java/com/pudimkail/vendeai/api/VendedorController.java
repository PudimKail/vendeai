package com.pudimkail.vendeai.api;

import com.pudimkail.vendeai.api.Dtos.ProdutoDto;
import com.pudimkail.vendeai.api.Dtos.VendaDto;
import com.pudimkail.vendeai.pedido.PedidoService;
import com.pudimkail.vendeai.produto.AnuncioForm;
import com.pudimkail.vendeai.produto.ProdutoService;
import com.pudimkail.vendeai.usuario.Usuario;
import com.pudimkail.vendeai.usuario.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/** Área do vendedor: cadastrar e editar anúncios, pausar e acompanhar as vendas. */
@RestController
@RequestMapping("/api")
public class VendedorController {

    private final UsuarioService usuarios;
    private final ProdutoService produtos;
    private final PedidoService pedidos;

    public VendedorController(UsuarioService usuarios, ProdutoService produtos, PedidoService pedidos) {
        this.usuarios = usuarios;
        this.produtos = produtos;
        this.pedidos = pedidos;
    }

    @GetMapping("/anuncios")
    public List<ProdutoDto> meusAnuncios(Principal principal) {
        return ProdutoDto.de(produtos.doVendedor(usuarios.exigir(principal)));
    }

    @GetMapping("/anuncios/{id}")
    public ProdutoDto anuncio(@PathVariable Long id, Principal principal) {
        return ProdutoDto.de(produtos.doDono(usuarios.exigir(principal), id));
    }

    /** Recebe o formulário com a foto (multipart/form-data). */
    @PostMapping(value = "/anuncios", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProdutoDto criar(@Valid @ModelAttribute AnuncioForm form,
                            @RequestPart(value = "foto", required = false) MultipartFile foto, Principal principal) {
        return ProdutoDto.de(produtos.criar(usuarios.exigirVerificado(principal), form, foto));
    }

    @PostMapping(value = "/anuncios/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProdutoDto editar(@PathVariable Long id, @Valid @ModelAttribute AnuncioForm form,
                             @RequestPart(value = "foto", required = false) MultipartFile foto, Principal principal) {
        return ProdutoDto.de(produtos.editar(usuarios.exigirVerificado(principal), id, form, foto));
    }

    @PostMapping("/anuncios/{id}/pausar")
    public ProdutoDto pausarOuAtivar(@PathVariable Long id, Principal principal) {
        Usuario u = usuarios.exigir(principal);
        produtos.alternarAtivo(u, id);
        return ProdutoDto.de(produtos.doDono(u, id));
    }

    @GetMapping("/vendas")
    public List<VendaDto> vendas(Principal principal) {
        Usuario u = usuarios.exigir(principal);
        return pedidos.vendas(u).stream().map(p -> VendaDto.de(p, u.getId())).toList();
    }

    @PostMapping("/vendas/{pedidoId}/enviar")
    public VendaDto enviar(@PathVariable Long pedidoId, Principal principal) {
        Usuario u = usuarios.exigir(principal);
        return VendaDto.de(pedidos.marcarEnviado(u, pedidoId), u.getId());
    }
}
