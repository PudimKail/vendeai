package com.pudimkail.vendeai.usuario;

import com.pudimkail.vendeai.notificacao.NotificacaoService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final int MAX_TENTATIVAS = 5;
    private static final int SEGUNDOS_PARA_REENVIAR = 60;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final NotificacaoService notificacoes;
    private final EmailService email;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder, NotificacaoService notificacoes,
                          EmailService email) {
        this.repository = repository;
        this.encoder = encoder;
        this.notificacoes = notificacoes;
        this.email = email;
    }

    /** Resultado do cadastro. O código só volta preenchido no modo demonstração (sem servidor de e-mail). */
    public record Cadastro(Usuario usuario, String codigoDemo) {
    }

    @Transactional
    public Cadastro cadastrar(CadastroForm form) {
        String emailNormalizado = form.getEmail().toLowerCase().trim();
        if (repository.existsByEmail(emailNormalizado)) {
            throw new IllegalArgumentException("Já existe uma conta com esse e-mail");
        }
        if (!form.getSenha().equals(form.getConfirmacaoSenha())) {
            throw new IllegalArgumentException("As senhas não são iguais");
        }
        Usuario usuario = repository.save(new Usuario(form.getNome().trim(), emailNormalizado,
                encoder.encode(form.getSenha()), form.getTelefone()));
        String codigo = enviarNovoCodigo(usuario);
        notificacoes.notificar(usuario, "👋", "Bem-vindo(a) ao VendeAí! Que tal anunciar seu primeiro produto?", "/vender");
        return new Cadastro(usuario, email.isModoDemonstracao() ? codigo : null);
    }

    /**
     * Confere o código digitado. Depois de 5 erros, é preciso pedir um código novo.
     * noRollbackFor: a tentativa errada precisa ficar salva mesmo quando a resposta é um erro.
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verificarEmail(Usuario logado, String codigo) {
        Usuario usuario = repository.findById(logado.getId()).orElseThrow();
        if (usuario.isEmailVerificado()) {
            return;
        }
        if (usuario.getCodigoHash() == null || usuario.getCodigoExpiraEm().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código expirado. Peça um novo código.");
        }
        if (usuario.getTentativasCodigo() >= MAX_TENTATIVAS) {
            throw new IllegalArgumentException("Muitas tentativas erradas. Peça um novo código.");
        }
        usuario.registrarTentativa();
        if (codigo == null || !encoder.matches(codigo.trim(), usuario.getCodigoHash())) {
            int restantes = MAX_TENTATIVAS - usuario.getTentativasCodigo();
            throw new IllegalArgumentException("Código incorreto. Você ainda tem " + restantes + " tentativa(s).");
        }
        usuario.confirmarEmail();
        notificacoes.notificar(usuario, "✉️", "E-mail confirmado! Agora você já pode comprar e vender.", "/");
    }

    /** Reenvia o código (no máximo um por minuto). Devolve o código só no modo demonstração. */
    @Transactional
    public String reenviarCodigo(Usuario logado) {
        Usuario usuario = repository.findById(logado.getId()).orElseThrow();
        if (usuario.isEmailVerificado()) {
            throw new IllegalArgumentException("Seu e-mail já está confirmado");
        }
        LocalDateTime enviado = usuario.getCodigoEnviadoEm();
        if (enviado != null && enviado.plusSeconds(SEGUNDOS_PARA_REENVIAR).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Aguarde 1 minuto para pedir outro código");
        }
        String codigo = enviarNovoCodigo(usuario);
        return email.isModoDemonstracao() ? codigo : null;
    }

    public boolean isEmailModoDemonstracao() {
        return email.isModoDemonstracao();
    }

    private String enviarNovoCodigo(Usuario usuario) {
        String codigo = "%06d".formatted(ALEATORIO.nextInt(1_000_000));
        usuario.novoCodigo(encoder.encode(codigo), LocalDateTime.now());
        email.enviarCodigo(usuario, codigo);
        return codigo;
    }

    /** Usuário logado na requisição, se houver. */
    public Optional<Usuario> atual(Principal principal) {
        return principal == null ? Optional.empty() : repository.findByEmail(principal.getName());
    }

    public Usuario exigir(Principal principal) {
        return atual(principal).orElseThrow(() -> new IllegalStateException("Faça login para continuar"));
    }

    /** Comprar e vender só depois de confirmar o e-mail. */
    public Usuario exigirVerificado(Principal principal) {
        Usuario usuario = exigir(principal);
        if (!usuario.isEmailVerificado()) {
            throw new EmailNaoVerificadoException();
        }
        return usuario;
    }
}
