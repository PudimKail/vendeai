package com.pudimkail.vendeai.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senhaHash;

    private String telefone;

    private LocalDateTime criadoEm = LocalDateTime.now();

    // ---- Verificação de e-mail ----
    private boolean emailVerificado;
    /** Hash do código de 6 dígitos (o código em si não fica salvo). */
    private String codigoHash;
    private LocalDateTime codigoExpiraEm;
    private LocalDateTime codigoEnviadoEm;
    private int tentativasCodigo;

    protected Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, String telefone) {
        this.nome = nome;
        this.email = email.toLowerCase().trim();
        this.senhaHash = senhaHash;
        this.telefone = telefone;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public String getTelefone() { return telefone; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public String getPrimeiroNome() {
        return nome.split("\\s+")[0];
    }

    void novoCodigo(String hash, LocalDateTime agora) {
        this.codigoHash = hash;
        this.codigoEnviadoEm = agora;
        this.codigoExpiraEm = agora.plusMinutes(15);
        this.tentativasCodigo = 0;
    }

    void registrarTentativa() { this.tentativasCodigo++; }

    void confirmarEmail() {
        this.emailVerificado = true;
        this.codigoHash = null;
        this.codigoExpiraEm = null;
    }

    /** Para os dados de exemplo (contas de demonstração já verificadas). */
    public void marcarEmailVerificado() { confirmarEmail(); }

    public boolean isEmailVerificado() { return emailVerificado; }
    String getCodigoHash() { return codigoHash; }
    LocalDateTime getCodigoExpiraEm() { return codigoExpiraEm; }
    LocalDateTime getCodigoEnviadoEm() { return codigoEnviadoEm; }
    int getTentativasCodigo() { return tentativasCodigo; }
}
