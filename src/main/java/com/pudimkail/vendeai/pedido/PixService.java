package com.pudimkail.vendeai.pedido;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Gera o "Pix copia e cola" no formato do Banco Central (BR Code / EMV) e o QR Code.
 * MODO DEMONSTRAÇÃO: a chave Pix é aleatória, então ninguém consegue pagar de verdade.
 * Para receber pagamentos reais, troque por um provedor (Mercado Pago, Asaas, Efí...).
 */
@Service
public class PixService {

    public String gerarCodigo(Long pedidoId, BigDecimal valor) {
        String chaveDemo = UUID.nameUUIDFromBytes(("vendeai-" + pedidoId).getBytes(StandardCharsets.UTF_8)).toString();
        String contaPix = campo("00", "br.gov.bcb.pix") + campo("01", chaveDemo);
        String semCrc = campo("00", "01")
                + campo("26", contaPix)
                + campo("52", "0000")
                + campo("53", "986") // real
                + campo("54", valor.setScale(2).toPlainString())
                + campo("58", "BR")
                + campo("59", "VENDEAI DEMO")
                + campo("60", "SAO PAULO")
                + campo("62", campo("05", "PEDIDO" + pedidoId))
                + "6304";
        return semCrc + crc16(semCrc);
    }

    /** Imagem PNG do QR Code em base64, pronta para usar em <img src="...">. */
    public String qrCodeDataUri(String texto) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, 260, 260,
                    Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage imagem = new BufferedImage(matriz.getWidth(), matriz.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matriz.getWidth(); x++) {
                for (int y = 0; y < matriz.getHeight(); y++) {
                    imagem.setRGB(x, y, matriz.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(imagem, "png", png);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Erro ao gerar QR Code", e);
        }
    }

    /** Campo EMV: ID (2 dígitos) + tamanho (2 dígitos) + valor. */
    private static String campo(String id, String valor) {
        return id + "%02d".formatted(valor.length()) + valor;
    }

    /** CRC16-CCITT (polinômio 0x1021), exigido no final do código Pix. */
    static String crc16(String texto) {
        int crc = 0xFFFF;
        for (byte b : texto.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
            }
        }
        return "%04X".formatted(crc & 0xFFFF);
    }
}
