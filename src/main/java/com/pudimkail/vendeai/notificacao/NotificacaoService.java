package com.pudimkail.vendeai.notificacao;

import com.pudimkail.vendeai.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacaoService {

    private final NotificacaoRepository repository;

    public NotificacaoService(NotificacaoRepository repository) {
        this.repository = repository;
    }

    public void notificar(Usuario usuario, String icone, String mensagem, String link) {
        repository.save(new Notificacao(usuario, icone, mensagem, link));
    }

    public List<Notificacao> listar(Usuario usuario) {
        return repository.findTop50ByUsuarioOrderByCriadoEmDesc(usuario);
    }

    public long naoLidas(Usuario usuario) {
        return repository.countByUsuarioAndLidaFalse(usuario);
    }

    @Transactional
    public void marcarTodasComoLidas(Usuario usuario) {
        repository.findByUsuarioAndLidaFalse(usuario).forEach(Notificacao::marcarLida);
    }
}
