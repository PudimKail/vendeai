import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { get } from '../api'
import { dataHora, moeda } from '../formato'

export default function MinhasCompras() {
  const [pedidos, setPedidos] = useState(null)

  useEffect(() => {
    get('/pedidos').then(setPedidos)
  }, [])

  if (!pedidos) return <div className="carregando" />

  return (
    <div className="pagina-media">
      <h1>Minhas compras</h1>
      {pedidos.length === 0 && (
        <div className="vazio">
          <span>🧾</span>
          <p>Você ainda não fez nenhuma compra.</p>
          <Link to="/busca" className="botao botao-principal">Começar a comprar</Link>
        </div>
      )}
      {pedidos.map((p) => (
        <Link key={p.id} to={`/compras/${p.id}`} className="painel cartao-pedido">
          <div>
            <span className={'status status-' + p.status.toLowerCase()}>{p.statusRotulo}</span>
            <h3>{p.itens.map((i) => i.titulo).join(', ')}</h3>
            <small className="texto-suave">Pedido #{p.id} · {dataHora(p.criadoEm)} · {p.formaPagamentoRotulo}</small>
          </div>
          <strong>{moeda(p.total)}</strong>
        </Link>
      ))}
    </div>
  )
}
