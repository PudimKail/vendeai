package com.pudimkail.vendeai.carrinho;

import com.pudimkail.vendeai.produto.Produto;
import com.pudimkail.vendeai.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, Long> {

    List<ItemCarrinho> findByUsuarioOrderById(Usuario usuario);

    Optional<ItemCarrinho> findByUsuarioAndProduto(Usuario usuario, Produto produto);

    Optional<ItemCarrinho> findByIdAndUsuario(Long id, Usuario usuario);

    @Query("select coalesce(sum(i.quantidade), 0) from ItemCarrinho i where i.usuario = :usuario")
    int contarItens(Usuario usuario);

    void deleteByUsuario(Usuario usuario);
}
