package com.pudimkail.vendeai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa a API pelo HTTP, como o React usa: segurança, login por sessão e JSON. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTests {

    @Autowired
    MockMvc mvc;

    @Test
    void vitrineEPublicaECarrinhoExigeLogin() throws Exception {
        mvc.perform(get("/api/produtos").param("q", "notebook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(3))
                .andExpect(jsonPath("$.itens[0].vendedor.nome").exists())
                .andExpect(jsonPath("$.itens[0].vendedor.senhaHash").doesNotExist());
        mvc.perform(get("/api/carrinho")).andExpect(status().isUnauthorized());
    }

    @Test
    void postSemTokenCsrfEhBloqueado() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"comprador@vendeai.dev\",\"senha\":\"demo1234\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginComSessaoEAdicionarAoCarrinho() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"comprador@vendeai.dev\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("E-mail ou senha incorretos"));

        MockHttpSession sessao = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"COMPRADOR@vendeai.dev\",\"senha\":\"demo1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.primeiroNome").value("Comprador"));

        mvc.perform(get("/api/auth/eu").session(sessao))
                .andExpect(jsonPath("$.usuario.email").value("comprador@vendeai.dev"));
    }

    @Test
    void cadastroDevolveCodigoNoModoDemonstracaoEBloqueiaCompra() throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        mvc.perform(post("/api/auth/cadastro").session(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana Souza","email":"ana@teste.com","senha":"segredo1","confirmacaoSenha":"segredo1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.emailVerificado").value(false))
                .andExpect(jsonPath("$.codigoDemo").isString());

        mvc.perform(post("/api/carrinho").session(sessao).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":1,\"quantidade\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("EMAIL_NAO_VERIFICADO"));
    }

    @Test
    void cadastroInvalidoMostraMensagem() throws Exception {
        mvc.perform(post("/api/auth/cadastro").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ze\",\"email\":\"nao-e-email\",\"senha\":\"123\",\"confirmacaoSenha\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").value("E-mail inválido"));
    }

    @Test
    void rotasDoReactAbremOIndex() throws Exception {
        mvc.perform(get("/produto/5")).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/busca")).andExpect(forwardedUrl("/index.html"));
    }
}
