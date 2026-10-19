import { Link } from 'react-router-dom'

export default function Rodape() {
  return (
    <footer className="rodape">
      <div className="rodape-colunas">
        <div>
          <strong>🛍️ VendeAí</strong>
          <p>Compre e venda de tudo, com segurança, Pix e cartão.</p>
        </div>
        <div>
          <strong>Comprar</strong>
          <Link to="/busca">Todos os produtos</Link>
          <Link to="/busca?freteGratis=true">Frete grátis</Link>
          <Link to="/compras">Minhas compras</Link>
        </div>
        <div>
          <strong>Vender</strong>
          <Link to="/vender">Anunciar produto</Link>
          <Link to="/anuncios">Meus anúncios</Link>
          <Link to="/vendas">Minhas vendas</Link>
        </div>
      </div>
      <p className="rodape-aviso">
        Projeto de portfólio feito com Java, Spring Boot, PostgreSQL e React. Os pagamentos são simulados: nada é cobrado.
      </p>
    </footer>
  )
}
