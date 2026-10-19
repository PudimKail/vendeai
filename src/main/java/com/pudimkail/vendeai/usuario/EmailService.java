package com.pudimkail.vendeai.usuario;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Envia o código de verificação por e-mail.
 * Sem servidor de e-mail configurado (MAIL_HOST), funciona em MODO DEMONSTRAÇÃO:
 * o código vai para o log e é devolvido para a tela, para dar para testar o cadastro.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String remetente;

    public EmailService(ObjectProvider<JavaMailSender> mailSender, @Value("${spring.mail.host:}") String host,
                        @Value("${spring.mail.username:}") String remetente) {
        this.mailSender = host.isBlank() ? null : mailSender.getIfAvailable();
        this.remetente = remetente;
    }

    public boolean isModoDemonstracao() {
        return mailSender == null;
    }

    public void enviarCodigo(Usuario usuario, String codigo) {
        if (isModoDemonstracao()) {
            log.info("[MODO DEMONSTRAÇÃO] Código de verificação de {}: {}", usuario.getEmail(), codigo);
            return;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(usuario.getEmail());
        mensagem.setSubject(codigo + " é o seu código de verificação do VendeAí");
        mensagem.setText("""
                Olá, %s!

                Seu código de verificação é: %s

                Ele vale por 15 minutos. Se você não criou uma conta no VendeAí, ignore este e-mail.
                """.formatted(usuario.getPrimeiroNome(), codigo));
        try {
            mailSender.send(mensagem);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail para {}", usuario.getEmail(), e);
            throw new IllegalArgumentException("Não conseguimos enviar o e-mail agora. Tente reenviar o código.");
        }
    }
}
