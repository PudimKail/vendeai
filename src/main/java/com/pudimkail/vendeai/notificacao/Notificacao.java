package com.pudimkail.vendeai.notificacao;

import com.pudimkail.vendeai.usuario.Usuario;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Usuario usuario;

    private String icone;
    private String mensagem;
    private String link;
    private boolean lida;
    private LocalDateTime criadoEm = LocalDateTime.now();

    protected Notificacao() {
    }

    public Notificacao(Usuario usuario, String icone, String mensagem, String link) {
        this.usuario = usuario;
        this.icone = icone;
        this.mensagem = mensagem;
        this.link = link;
    }

    void marcarLida() { this.lida = true; }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getIcone() { return icone; }
    public String getMensagem() { return mensagem; }
    public String getLink() { return link; }
    public boolean isLida() { return lida; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
}
