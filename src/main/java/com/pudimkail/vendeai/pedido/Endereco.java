package com.pudimkail.vendeai.pedido;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Embeddable
public class Endereco {

    @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido")
    private String cep;

    @NotBlank(message = "Informe a rua")
    private String rua;

    @NotBlank(message = "Informe o número")
    private String numero;

    private String complemento;

    @NotBlank(message = "Informe o bairro")
    private String bairro;

    @NotBlank(message = "Informe a cidade")
    private String cidade;

    @Size(min = 2, max = 2, message = "UF com 2 letras")
    private String uf;

    public String getResumo() {
        return "%s, %s%s - %s, %s/%s - CEP %s".formatted(rua, numero,
                complemento == null || complemento.isBlank() ? "" : " (" + complemento + ")", bairro, cidade,
                uf == null ? "" : uf.toUpperCase(), cep);
    }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }
    public String getRua() { return rua; }
    public void setRua(String rua) { this.rua = rua; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }
    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }
    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public String getUf() { return uf; }
    public void setUf(String uf) { this.uf = uf; }
}
