package com.pudimkail.vendeai.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CadastroForm {

    @NotBlank(message = "Informe seu nome")
    @Size(max = 80)
    private String nome;

    @NotBlank(message = "Informe seu e-mail")
    @Email(message = "E-mail inválido")
    private String email;

    @Pattern(regexp = "^$|^\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}$", message = "Telefone inválido. Ex.: (65) 99999-0000")
    private String telefone;

    @Size(min = 6, max = 72, message = "A senha precisa ter pelo menos 6 caracteres")
    private String senha;

    private String confirmacaoSenha;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getConfirmacaoSenha() { return confirmacaoSenha; }
    public void setConfirmacaoSenha(String confirmacaoSenha) { this.confirmacaoSenha = confirmacaoSenha; }
}
