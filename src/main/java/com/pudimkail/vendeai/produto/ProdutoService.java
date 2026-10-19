package com.pudimkail.vendeai.produto;

import com.pudimkail.vendeai.usuario.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProdutoService {

    public static final int POR_PAGINA = 12;
    private static final Map<String, String> TIPOS_DE_FOTO = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");

    private final ProdutoRepository produtos;
    private final CategoriaRepository categorias;
    private final Path pastaUploads;

    public ProdutoService(ProdutoRepository produtos, CategoriaRepository categorias,
                          @Value("${vendeai.uploads}") String pastaUploads) {
        this.produtos = produtos;
        this.categorias = categorias;
        this.pastaUploads = Path.of(pastaUploads).toAbsolutePath();
    }

    /** Busca com filtros opcionais, ordenação e paginação de 12 em 12. */
    public Page<Produto> buscar(Filtros f, int pagina) {
        Specification<Produto> spec = (root, query, cb) -> cb.isTrue(root.get("ativo"));

        if (f.q() != null && !f.q().isBlank()) {
            // Cada palavra digitada precisa aparecer no título ou na descrição
            for (String palavra : f.q().toLowerCase().trim().split("\\s+")) {
                String like = "%" + palavra + "%";
                spec = spec.and((root, query, cb) -> cb.or(
                        cb.like(cb.lower(root.get("titulo")), like),
                        cb.like(cb.lower(root.get("descricao")), like)));
            }
        }
        if (f.categoria() != null && !f.categoria().isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("categoria").get("slug"), f.categoria()));
        }
        if (f.min() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("preco"), f.min()));
        }
        if (f.max() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("preco"), f.max()));
        }
        if (f.condicao() != null && !f.condicao().isBlank()) {
            Produto.Condicao condicao = Produto.Condicao.valueOf(f.condicao());
            spec = spec.and((root, query, cb) -> cb.equal(root.get("condicao"), condicao));
        }
        if (f.freteGratis()) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("freteGratis")));
        }

        Sort ordem = switch (f.getOrdemOuPadrao()) {
            case Filtros.MENOR_PRECO -> Sort.by("preco").ascending();
            case Filtros.MAIOR_PRECO -> Sort.by("preco").descending();
            case Filtros.RECENTES -> Sort.by("criadoEm").descending();
            default -> Sort.by("vendidos").descending().and(Sort.by("id").descending());
        };
        return produtos.findAll(spec, PageRequest.of(Math.max(pagina, 0), POR_PAGINA, ordem));
    }

    public Produto buscarPorId(Long id) {
        return produtos.findById(id).orElseThrow(() -> new NaoEncontradoException("Produto não encontrado"));
    }

    public List<Produto> maisVendidos() {
        return produtos.findTop8ByAtivoTrueAndEstoqueGreaterThanOrderByVendidosDesc(0);
    }

    public List<Produto> recentes() {
        return produtos.findTop8ByAtivoTrueAndEstoqueGreaterThanOrderByCriadoEmDesc(0);
    }

    public List<Produto> semelhantes(Produto produto) {
        return produtos.findTop4ByCategoriaAndAtivoTrueAndIdNotOrderByVendidosDesc(produto.getCategoria(), produto.getId());
    }

    public List<Produto> doVendedor(Usuario vendedor) {
        return produtos.findByVendedorOrderByCriadoEmDesc(vendedor);
    }

    public List<Categoria> categorias() {
        return categorias.findAllByOrderByNome();
    }

    @Transactional
    public Produto criar(Usuario vendedor, AnuncioForm form, MultipartFile foto) {
        Produto produto = new Produto(form.getTitulo(), form.getDescricao(), form.getPreco(), form.getEstoque(),
                form.getCondicao(), categoria(form.getCategoriaId()), vendedor, form.isFreteGratis());
        produto.setImagem(salvarFoto(foto));
        return produtos.save(produto);
    }

    @Transactional
    public Produto editar(Usuario vendedor, Long id, AnuncioForm form, MultipartFile foto) {
        Produto produto = doDono(vendedor, id);
        produto.atualizar(form.getTitulo(), form.getDescricao(), form.getPreco(), form.getEstoque(),
                form.getCondicao(), categoria(form.getCategoriaId()), form.isFreteGratis());
        String novaFoto = salvarFoto(foto);
        if (novaFoto != null) {
            produto.setImagem(novaFoto);
        }
        return produto;
    }

    @Transactional
    public void alternarAtivo(Usuario vendedor, Long id) {
        Produto produto = doDono(vendedor, id);
        produto.setAtivo(!produto.isAtivo());
    }

    /** Só o dono do anúncio pode editar ou pausar. */
    public Produto doDono(Usuario vendedor, Long id) {
        Produto produto = buscarPorId(id);
        if (!produto.pertenceA(vendedor)) {
            throw new NaoEncontradoException("Anúncio não encontrado");
        }
        return produto;
    }

    private Categoria categoria(Long id) {
        return categorias.findById(id).orElseThrow(() -> new IllegalArgumentException("Categoria inválida"));
    }

    /** Salva a foto com um nome aleatório. Aceita só JPG, PNG e WEBP. */
    private String salvarFoto(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            return null;
        }
        String extensao = TIPOS_DE_FOTO.get(foto.getContentType());
        if (extensao == null) {
            throw new IllegalArgumentException("A foto precisa ser JPG, PNG ou WEBP");
        }
        String nome = UUID.randomUUID() + extensao;
        try (InputStream in = foto.getInputStream()) {
            Files.createDirectories(pastaUploads);
            Files.copy(in, pastaUploads.resolve(nome), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível salvar a foto", e);
        }
        return nome;
    }
}
