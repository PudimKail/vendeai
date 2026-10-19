package com.pudimkail.vendeai.pedido;

import com.pudimkail.vendeai.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByCompradorOrderByCriadoEmDesc(Usuario comprador);

    Optional<Pedido> findByIdAndComprador(Long id, Usuario comprador);

    /** Pedidos pagos que têm pelo menos um produto do vendedor. */
    @Query("""
            select distinct p from Pedido p join p.itens i
            where i.vendedorId = :vendedorId and p.status <> com.pudimkail.vendeai.pedido.Pedido.Status.AGUARDANDO_PAGAMENTO
              and p.status <> com.pudimkail.vendeai.pedido.Pedido.Status.CANCELADO
            order by p.id desc
            """)
    List<Pedido> findVendasDoVendedor(Long vendedorId);

    /** O comprador recebeu esse produto? Só assim ele pode avaliar. */
    @Query("""
            select count(p) > 0 from Pedido p join p.itens i
            where p.comprador = :comprador and i.produtoId = :produtoId
              and p.status = com.pudimkail.vendeai.pedido.Pedido.Status.ENTREGUE
            """)
    boolean recebeuProduto(Usuario comprador, Long produtoId);
}
