package com.pudimkail.vendeai.avaliacao;

import com.pudimkail.vendeai.produto.Produto;
import com.pudimkail.vendeai.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    List<Avaliacao> findByProdutoOrderByCriadoEmDesc(Produto produto);

    boolean existsByProdutoAndAutor(Produto produto, Usuario autor);

    List<Avaliacao> findByAutor(Usuario autor);
}
