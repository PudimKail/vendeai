import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { del, get, put } from '../api'
import { moeda } from '../formato'
import { useSessao } from '../sessao'
import { ImagemProduto } from '../componentes/Produto'

export default function Carrinho() {
  const { recarregar, avisar } = useSessao()
  const [carrinho, setCarrinho] = useState(null)

  useEffect(() => {
    get('/carrinho').then(setCarrinho)
  }, [])

  const alterar = async (item, quantidade) => {
    try {
      setCarrinho(quantidade > 0 ? await put(`/carrinho/${item.id}`, { quantidade }) : await del(`/carrinho/${item.id}`))
      recarregar()
    } catch (e) {
      avisar(e.message, 'erro')
    }
  }

  if (!carrinho) return <div className="carregando" />

  if (carrinho.itens.length === 0) {
    return (
      <div className="vazio">
        <span>🛒</span>
        <h1>Seu carrinho está vazio</h1>
        <p>Que tal dar uma olhada nas ofertas?</p>
        <Link to="/busca" className="botao botao-principal">Ver produtos</Link>
      </div>
    )
  }

  const percentualFrete = carrinho.frete > 0 ? Math.min(100, (carrinho.subtotal / 79) * 100) : 100

  return (
    <div className="carrinho-layout">
      <section className="painel">
        <h1>Carrinho</h1>
        {carrinho.itens.map((item) => (
          <div key={item.id} className="item-carrinho">
            <Link to={`/produto/${item.produto.id}`}><ImagemProduto produto={item.produto} /></Link>
            <div className="item-info">
              <Link to={`/produto/${item.produto.id}`}>{item.produto.titulo}</Link>
              <small className="texto-suave">Vendido por {item.produto.vendedor.nome}</small>
              {item.produto.freteGratis && <small className="frete-gratis">Frete grátis</small>}
              <button className="botao-link" onClick={() => alterar(item, 0)}>Excluir</button>
            </div>
            <div className="contador-quantidade" aria-label="Quantidade">
              <button onClick={() => alterar(item, item.quantidade - 1)} aria-label="Diminuir">−</button>
              <span>{item.quantidade}</span>
              <button onClick={() => alterar(item, item.quantidade + 1)} disabled={item.quantidade >= item.produto.estoque} aria-label="Aumentar">+</button>
            </div>
            <strong className="item-preco">{moeda(item.subtotal)}</strong>
          </div>
        ))}
      </section>

      <aside className="painel resumo">
        <h2>Resumo da compra</h2>
        <div className="linha"><span>Produtos</span><span>{moeda(carrinho.subtotal)}</span></div>
        <div className="linha"><span>Frete</span><span className={carrinho.frete > 0 ? '' : 'frete-gratis'}>{carrinho.frete > 0 ? moeda(carrinho.frete) : 'Grátis'}</span></div>
        {carrinho.faltaParaFreteGratis > 0 && (
          <div className="barra-frete">
            <small>Faltam <strong>{moeda(carrinho.faltaParaFreteGratis)}</strong> para ganhar frete grátis</small>
            <div className="barra"><div style={{ width: percentualFrete + '%' }} /></div>
          </div>
        )}
        <div className="linha total"><span>Total</span><span>{moeda(carrinho.total)}</span></div>
        <Link to="/checkout" className="botao botao-principal botao-largo">Continuar a compra</Link>
      </aside>
    </div>
  )
}
