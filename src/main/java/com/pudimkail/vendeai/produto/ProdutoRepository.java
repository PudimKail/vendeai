package com.pudimkail.vendeai.produto;

import com.pudimkail.vendeai.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    List<Produto> findTop8ByAtivoTrueAndEstoqueGreaterThanOrderByVendidosDesc(int estoque);

    List<Produto> findTop8ByAtivoTrueAndEstoqueGreaterThanOrderByCriadoEmDesc(int estoque);

    List<Produto> findTop4ByCategoriaAndAtivoTrueAndIdNotOrderByVendidosDesc(Categoria categoria, Long id);

    List<Produto> findByVendedorOrderByCriadoEmDesc(Usuario vendedor);
}
