import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { get } from '../api'
import { Carregando, GradeProdutos } from '../componentes/Produto'

export default function Inicio() {
  const [destaques, setDestaques] = useState(null)
  const [categorias, setCategorias] = useState([])

  useEffect(() => {
    get('/produtos/destaques').then(setDestaques)
    get('/categorias').then(setCategorias)
  }, [])

  return (
    <>
      <section className="banner">
        <div>
          <h1>Compre e venda de tudo em um só lugar</h1>
          <p>Frete grátis acima de R$ 79, Pix na hora e parcelamento em até 10x sem juros.</p>
          <div className="banner-botoes">
            <Link to="/busca" className="botao botao-claro">Ver ofertas</Link>
            <Link to="/vender" className="botao botao-destaque">Comece a vender</Link>
          </div>
        </div>
        <div className="banner-emoji" aria-hidden="true">🛍️</div>
      </section>

      <section className="vantagens">
        <div><span>🚚</span><strong>Frete grátis</strong><small>em compras acima de R$ 79</small></div>
        <div><span>⚡</span><strong>Pix na hora</strong><small>pagamento aprovado em segundos</small></div>
        <div><span>💳</span><strong>10x sem juros</strong><small>no cartão de crédito</small></div>
        <div><span>🔒</span><strong>Compra segura</strong><small>seu dinheiro protegido</small></div>
      </section>

      <section>
        <h2>Categorias</h2>
        <div className="categorias-grade">
          {categorias.map((c) => (
            <Link key={c.slug} to={`/busca?categoria=${c.slug}`} className="categoria-cartao">
              <span>{c.icone}</span>
              {c.nome}
            </Link>
          ))}
        </div>
      </section>

      <section>
        <div className="secao-titulo">
          <h2>Mais vendidos</h2>
          <Link to="/busca?ordem=relevancia">Ver todos</Link>
        </div>
        {destaques ? <GradeProdutos produtos={destaques.maisVendidos} /> : <Carregando />}
      </section>

      <section>
        <div className="secao-titulo">
          <h2>Novidades</h2>
          <Link to="/busca?ordem=recentes">Ver todos</Link>
        </div>
        {destaques ? <GradeProdutos produtos={destaques.recentes} /> : <Carregando />}
      </section>
    </>
  )
}
