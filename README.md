# VendeAí 🛍️

Marketplace de compra e venda no estilo dos grandes e-commerces brasileiros: qualquer pessoa cria uma
conta, anuncia produtos, compra de outros vendedores, paga com Pix ou cartão, acompanha a entrega e
avalia o que comprou.

**Back-end:** Java 21 · Spring Boot 3 · Spring Security · Spring Data JPA · PostgreSQL
**Front-end:** React 18 · React Router · Vite

## Funcionalidades

**Comprador**
- Cadastro com **verificação de e-mail por código de 6 dígitos** (expira em 15 min, 5 tentativas, reenvio após 1 min)
- Busca por palavra, filtros (categoria, condição, faixa de preço, frete grátis), ordenação e paginação; filtros ficam na URL
- Página do produto com parcelamento, estoque, produtos semelhantes e avaliações
- Carrinho com frete grátis acima de R$ 79 (barra de progresso) e checkout com **CEP automático (ViaCEP)**
- Pagamento por **Pix** (código "copia e cola" no padrão do Banco Central + QR Code) ou **cartão de crédito** em até 10x
- Linha do tempo do pedido: feito → pago → a caminho → entregue
- **Avaliação** de 1 a 5 estrelas com comentário, só para quem recebeu o produto

**Vendedor**
- **Cadastro de produto** com foto (JPG/PNG/WEBP até 5 MB), edição, pausar e reativar anúncio
- Minhas vendas: total vendido, endereço de entrega e botão "Marcar como enviado"
- Pedidos com produtos de vários vendedores: cada um envia a sua parte

**Notificações** 🔔 em tempo real (o sininho atualiza sozinho): venda feita, pagamento aprovado, produto enviado,
recebimento confirmado e nova avaliação.

## Segurança

- Senhas com hash **BCrypt**; o código de verificação também é salvo só como hash
- Login por sessão (cookie httpOnly) e proteção **CSRF** em todas as requisições que alteram dados
- Só quem confirmou o e-mail pode comprar e vender; ninguém compra o próprio anúncio
- Preços e totais sempre calculados no servidor; o estoque só baixa quando o pagamento é aprovado, dentro de uma transação
- O número completo do cartão e o CVV nunca são salvos (só os 4 últimos dígitos)

## Modo demonstração

Para rodar sem custos e sem contas externas:

- **Pagamentos são simulados.** Cartões de teste: `4111 1111 1111 1111` (aprovado) e `4000 0000 0000 0002` (recusado).
  No Pix, o botão "Já paguei (simular)" faz o papel do banco. Para receber de verdade, troque `PixService` e
  `CartaoDemo` por um provedor (Mercado Pago, Asaas, Efí...).
- **E-mail:** sem servidor configurado, o código de verificação aparece na tela e no log. Para enviar e-mails de
  verdade (ex.: Gmail com "senha de app"), configure `MAIL_HOST`, `MAIL_USERNAME` e `MAIL_PASSWORD`.
- **Contas prontas** (senha `demo1234`): `comprador@vendeai.dev`, `loja@vendeai.dev`, `techstore@vendeai.dev`.

## Como rodar

Precisa de Java 21+, Maven, Node.js 20+ e PostgreSQL.

```bash
# 1. Banco
psql -U postgres -c "CREATE DATABASE vendeai;"

# 2. Front-end (gera os arquivos dentro do Spring Boot)
cd frontend
npm install
npm run build
cd ..

# 3. Back-end (Windows PowerShell)
$env:DB_PASSWORD="senha-do-postgres"
mvn spring-boot:run
```

Abra http://localhost:8081. Na primeira vez, o sistema cria categorias, lojas e produtos de exemplo.

Para desenvolver o front com recarga automática: deixe o back-end rodando e use `npm run dev` na pasta
`frontend` (abre em http://localhost:5173 e repassa as chamadas `/api` para a porta 8081).

| Variável | Para quê | Padrão |
|---|---|---|
| `DB_PASSWORD` | senha do PostgreSQL | vazio |
| `DB_USER` | usuário do PostgreSQL | `postgres` |
| `VENDEAI_DB_URL` | endereço do banco | `jdbc:postgresql://localhost:5432/vendeai` |
| `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD` | envio real do código por e-mail | modo demonstração |

## Testes

```bash
mvn test
```

12 testes de integração cobrem o fluxo inteiro (cadastro → código → carrinho → Pix/cartão → envio por dois
vendedores → entrega → avaliação), regras de negócio (estoque, cartão recusado, não comprar o próprio anúncio,
bloqueio após 5 códigos errados) e a API (CSRF, sessão, 401/403, validação, rotas do React).

## API

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/auth/eu` | usuário logado + contadores do carrinho e notificações |
| POST | `/api/auth/cadastro` · `/login` · `/sair` | conta e sessão |
| POST | `/api/auth/verificar-email` · `/reenviar-codigo` | código de verificação |
| GET | `/api/categorias` · `/api/produtos` · `/api/produtos/{id}` | vitrine e busca (públicos) |
| POST | `/api/produtos/{id}/avaliacoes` | avaliar produto recebido |
| GET/POST/PUT/DELETE | `/api/carrinho` | carrinho |
| POST/GET | `/api/pedidos` | finalizar compra / minhas compras |
| POST | `/api/pedidos/{id}/cartao` · `/pix` · `/cancelar` · `/recebido` | pagamento e entrega |
| GET/POST | `/api/anuncios` · `/api/anuncios/{id}` · `/pausar` | anúncios do vendedor (multipart com foto) |
| GET/POST | `/api/vendas` · `/api/vendas/{id}/enviar` | vendas do vendedor |
| GET/POST | `/api/notificacoes` · `/lidas` | notificações |
