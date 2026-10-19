package com.pudimkail.vendeai.api;

import com.pudimkail.vendeai.api.Dtos.NotificacaoDto;
import com.pudimkail.vendeai.notificacao.NotificacaoService;
import com.pudimkail.vendeai.usuario.UsuarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final UsuarioService usuarios;
    private final NotificacaoService notificacoes;

    public NotificacaoController(UsuarioService usuarios, NotificacaoService notificacoes) {
        this.usuarios = usuarios;
        this.notificacoes = notificacoes;
    }

    @GetMapping
    public List<NotificacaoDto> listar(Principal principal) {
        return notificacoes.listar(usuarios.exigir(principal)).stream().map(NotificacaoDto::de).toList();
    }

    @PostMapping("/lidas")
    public void marcarLidas(Principal principal) {
        notificacoes.marcarTodasComoLidas(usuarios.exigir(principal));
    }
}
