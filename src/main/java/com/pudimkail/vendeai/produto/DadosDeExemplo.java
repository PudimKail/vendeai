package com.pudimkail.vendeai.produto;

import com.pudimkail.vendeai.usuario.Usuario;
import com.pudimkail.vendeai.usuario.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Na primeira vez que o site sobe (banco vazio), cria categorias, lojas e produtos de exemplo.
 * Contas de demonstração (senha "demo1234"): loja@vendeai.dev, techstore@vendeai.dev, comprador@vendeai.dev
 */
@Component
public class DadosDeExemplo implements CommandLineRunner {

    public static final String SENHA_DEMO = "demo1234";
    private static final Logger log = LoggerFactory.getLogger(DadosDeExemplo.class);

    private final CategoriaRepository categorias;
    private final ProdutoRepository produtos;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public DadosDeExemplo(CategoriaRepository categorias, ProdutoRepository produtos, UsuarioRepository usuarios,
                          PasswordEncoder encoder) {
        this.categorias = categorias;
        this.produtos = produtos;
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categorias.count() > 0) {
            return;
        }
        Map<String, Categoria> c = Map.of(
                "celulares", categorias.save(new Categoria("Celulares", "celulares", "📱")),
                "informatica", categorias.save(new Categoria("Informática", "informatica", "💻")),
                "games", categorias.save(new Categoria("Games", "games", "🎮")),
                "eletrodomesticos", categorias.save(new Categoria("Eletrodomésticos", "eletrodomesticos", "🔌")),
                "casa", categorias.save(new Categoria("Casa e Móveis", "casa", "🛋️")),
                "moda", categorias.save(new Categoria("Moda", "moda", "👟")),
                "esportes", categorias.save(new Categoria("Esportes", "esportes", "⚽")),
                "ferramentas", categorias.save(new Categoria("Ferramentas", "ferramentas", "🛠️")));

        Usuario loja = conta("Loja do Centro", "loja@vendeai.dev");
        Usuario tech = conta("TechStore Brasil", "techstore@vendeai.dev");
        conta("Comprador Demo", "comprador@vendeai.dev");

        p(tech, c.get("celulares"), "Smartphone Galaxy A55 128GB 8GB RAM", "Tela Super AMOLED de 6,6\", câmera tripla de 50 MP, bateria de 5000 mAh e carregamento rápido. Acompanha cabo USB-C.", "1899.00", 12, true, 87);
        p(tech, c.get("celulares"), "iPhone 13 128GB Meia-noite", "Chip A15 Bionic, câmera dupla de 12 MP com modo noturno, tela Super Retina XDR de 6,1\". Garantia de 1 ano.", "3599.00", 5, true, 64);
        p(loja, c.get("celulares"), "Moto G84 256GB Azul", "Tela pOLED de 120 Hz, 256 GB de armazenamento, som Dolby Atmos e Android 14.", "1299.90", 20, true, 41);
        p(loja, c.get("celulares"), "Fone Bluetooth com Cancelamento de Ruído", "Até 30 horas de bateria, cancelamento ativo de ruído e microfone para chamadas.", "249.90", 40, false, 132);
        p(tech, c.get("informatica"), "Notebook Ryzen 5 16GB SSD 512GB 15,6\"", "Processador AMD Ryzen 5 7520U, 16 GB de RAM, SSD NVMe de 512 GB, tela Full HD antirreflexo e Windows 11.", "2899.00", 8, true, 53);
        p(tech, c.get("informatica"), "Monitor Gamer 24\" 165Hz IPS", "Painel IPS Full HD, 165 Hz, 1 ms, compatível com FreeSync e G-Sync.", "849.90", 15, true, 77);
        p(tech, c.get("informatica"), "Teclado Mecânico RGB Switch Blue", "Teclado ABNT2 com switches mecânicos, iluminação RGB e apoio de pulso.", "199.90", 30, false, 98);
        p(loja, c.get("informatica"), "Mouse sem Fio Ergonômico", "Sensor de 2400 DPI, conexão 2.4 GHz e pilha inclusa.", "59.90", 60, false, 210);
        p(tech, c.get("games"), "Console PlayStation 5 Slim Digital", "SSD de 1 TB, gráficos em 4K, controle DualSense incluso.", "3499.00", 4, true, 39);
        p(tech, c.get("games"), "Controle Sem Fio para Xbox Carbon Black", "Compatível com Xbox Series X|S, Xbox One e PC. Conexão Bluetooth.", "399.90", 18, true, 71);
        p(loja, c.get("games"), "Cadeira Gamer Reclinável Preta e Vermelha", "Encosto reclinável até 180°, almofadas para lombar e pescoço, suporta até 120 kg.", "749.00", 7, false, 26);
        p(loja, c.get("eletrodomesticos"), "Air Fryer 5,5 Litros Antiaderente", "Fritadeira sem óleo com timer de 60 minutos e controle de temperatura até 200 °C.", "349.90", 25, true, 156);
        p(loja, c.get("eletrodomesticos"), "Liquidificador 1200W 12 Velocidades", "Copo de 3 litros com filtro, lâminas de aço inox e função pulsar.", "189.90", 22, false, 88);
        p(tech, c.get("eletrodomesticos"), "Smart TV 50\" 4K UHD", "Wi-Fi, HDR10, assistente de voz e apps de streaming.", "2299.00", 6, true, 47);
        p(loja, c.get("casa"), "Jogo de Panelas Antiaderente 5 Peças", "Revestimento antiaderente, cabos que não esquentam, para todos os fogões menos indução.", "229.90", 14, false, 63);
        p(loja, c.get("casa"), "Kit 4 Toalhas de Banho 100% Algodão", "Toalhas macias e absorventes, 70 x 140 cm.", "119.90", 35, false, 91);
        p(loja, c.get("casa"), "Luminária de Mesa LED Articulada", "3 tons de luz, regulagem de brilho e porta USB para carregar o celular.", "89.90", 28, false, 44);
        p(loja, c.get("moda"), "Tênis de Corrida Masculino Leve", "Cabedal em mesh respirável e entressola com amortecimento.", "199.90", 30, true, 120);
        p(loja, c.get("moda"), "Mochila Executiva para Notebook 15,6\"", "Compartimento acolchoado, porta USB externa e tecido impermeável.", "149.90", 26, false, 73);
        p(loja, c.get("esportes"), "Bicicleta Aro 29 21 Marchas Freio a Disco", "Quadro de alumínio, suspensão dianteira e câmbio Shimano.", "1499.00", 5, true, 18);
        p(loja, c.get("esportes"), "Kit Halteres Ajustáveis 20 kg", "Barras e anilhas emborrachadas com presilhas de segurança.", "279.90", 12, false, 37);
        p(loja, c.get("ferramentas"), "Furadeira de Impacto 650W com Maleta", "Mandril de 13 mm, velocidade variável e kit de brocas.", "219.90", 16, true, 58);
        p(loja, c.get("ferramentas"), "Jogo de Chaves 46 Peças com Catraca", "Soquetes de 1/4\", catraca reversível e maleta organizadora.", "79.90", 40, false, 102);
        p(tech, c.get("informatica"), "Notebook Usado i5 8GB SSD 256GB", "Notebook usado em ótimo estado, bateria com boa duração, revisado e com Windows 11.", "1399.00", 2, false, 9, Produto.Condicao.USADO);

        log.info("Dados de exemplo criados: {} categorias e {} produtos", categorias.count(), produtos.count());
    }

    private Usuario conta(String nome, String email) {
        Usuario u = new Usuario(nome, email, encoder.encode(SENHA_DEMO), null);
        u.marcarEmailVerificado();
        return usuarios.save(u);
    }

    private void p(Usuario vendedor, Categoria categoria, String titulo, String descricao, String preco, int estoque,
                   boolean freteGratis, int vendidos) {
        p(vendedor, categoria, titulo, descricao, preco, estoque, freteGratis, vendidos, Produto.Condicao.NOVO);
    }

    private void p(Usuario vendedor, Categoria categoria, String titulo, String descricao, String preco, int estoque,
                   boolean freteGratis, int vendidos, Produto.Condicao condicao) {
        Produto produto = new Produto(titulo, descricao, new BigDecimal(preco), estoque, condicao, categoria, vendedor, freteGratis);
        produto.setVendidos(vendidos);
        produtos.save(produto);
    }
}
