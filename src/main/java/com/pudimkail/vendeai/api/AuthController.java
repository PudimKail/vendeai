package com.pudimkail.vendeai.api;

import com.pudimkail.vendeai.api.Dtos.CodigoRequest;
import com.pudimkail.vendeai.api.Dtos.LoginRequest;
import com.pudimkail.vendeai.api.Dtos.SessaoDto;
import com.pudimkail.vendeai.api.Dtos.UsuarioDto;
import com.pudimkail.vendeai.carrinho.CarrinhoService;
import com.pudimkail.vendeai.notificacao.NotificacaoService;
import com.pudimkail.vendeai.usuario.CadastroForm;
import com.pudimkail.vendeai.usuario.Usuario;
import com.pudimkail.vendeai.usuario.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final UsuarioService usuarios;
    private final CarrinhoService carrinho;
    private final NotificacaoService notificacoes;

    public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository contextRepository,
                          UsuarioService usuarios, CarrinhoService carrinho, NotificacaoService notificacoes) {
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.usuarios = usuarios;
        this.carrinho = carrinho;
        this.notificacoes = notificacoes;
    }

    /** O React chama ao abrir o site. Também gera o cookie XSRF-TOKEN usado nos POSTs. */
    @GetMapping("/eu")
    public SessaoDto eu(Principal principal, CsrfToken csrf) {
        csrf.getToken();
        return usuarios.atual(principal).map(u -> sessao(u, null)).orElse(SessaoDto.anonima());
    }

    @PostMapping("/login")
    public SessaoDto login(@RequestBody LoginRequest req, HttpServletRequest request, HttpServletResponse response) {
        entrar(req.email(), req.senha(), request, response);
        return sessao(usuarios.exigir(() -> req.email().toLowerCase().trim()), null);
    }

    /** Cria a conta (ainda sem e-mail confirmado), já faz o login e envia o código por e-mail. */
    @PostMapping("/cadastro")
    public SessaoDto cadastro(@Valid @RequestBody CadastroForm form, HttpServletRequest request, HttpServletResponse response) {
        UsuarioService.Cadastro cadastro = usuarios.cadastrar(form);
        entrar(cadastro.usuario().getEmail(), form.getSenha(), request, response);
        return sessao(cadastro.usuario(), cadastro.codigoDemo());
    }

    @PostMapping("/verificar-email")
    public SessaoDto verificarEmail(@RequestBody CodigoRequest req, Principal principal) {
        usuarios.verificarEmail(usuarios.exigir(principal), req.codigo());
        return sessao(usuarios.exigir(principal), null);
    }

    @PostMapping("/reenviar-codigo")
    public SessaoDto reenviarCodigo(Principal principal) {
        Usuario usuario = usuarios.exigir(principal);
        String codigoDemo = usuarios.reenviarCodigo(usuario);
        return sessao(usuario, codigoDemo);
    }

    @PostMapping("/sair")
    public SessaoDto sair(HttpServletRequest request, HttpServletResponse response, Authentication auth) {
        new SecurityContextLogoutHandler().logout(request, response, auth);
        return SessaoDto.anonima();
    }

    /** Confere e-mail e senha e guarda o login na sessão (cookie JSESSIONID). */
    private void entrar(String email, String senha, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email == null ? "" : email.toLowerCase().trim(), senha));
        if (request.getSession(false) != null) {
            request.changeSessionId(); // evita fixação de sessão
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(auth);
        SecurityContextHolder.setContext(contexto);
        contextRepository.saveContext(contexto, request, response);
    }

    private SessaoDto sessao(Usuario u, String codigoDemo) {
        return new SessaoDto(UsuarioDto.de(u), carrinho.quantidade(u), notificacoes.naoLidas(u), codigoDemo);
    }
}
