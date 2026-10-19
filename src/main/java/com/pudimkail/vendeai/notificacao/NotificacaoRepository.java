package com.pudimkail.vendeai.notificacao;

import com.pudimkail.vendeai.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findTop50ByUsuarioOrderByCriadoEmDesc(Usuario usuario);

    long countByUsuarioAndLidaFalse(Usuario usuario);

    List<Notificacao> findByUsuarioAndLidaFalse(Usuario usuario);
}
