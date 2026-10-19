import { useEffect, useState } from 'react'
import { get, post } from '../api'
import { dataHora, moeda } from '../formato'
import { useSessao } from '../sessao'

export default function MinhasVendas() {
  const { avisar } = useSessao()
  const [vendas, setVendas] = useState(null)

  useEffect(() => {
    get('/vendas').then(setVendas)
  }, [])

  const enviar = async (venda) => {
    try {
      const atualizada = await post(`/vendas/${venda.pedidoId}/enviar`)
      setVendas(vendas.map((v) => (v.pedidoId === atualizada.pedidoId ? atualizada : v)))
      avisar('🚚 Envio registrado. O comprador foi avisado!')
    } catch (e) {
      avisar(e.message, 'erro')
    }
  }

  if (!vendas) return <div className="carregando" />

  const totalVendido = vendas.reduce((soma, v) => soma + Number(v.total), 0)
  const aEnviar = vendas.filter((v) => !v.enviado).length

  return (
    <div className="pagina-media">
      <h1>Minhas vendas</h1>
      <div className="indicadores">
        <div className="painel"><small>Total vendido</small><strong>{moeda(totalVendido)}</strong></div>
        <div className="painel"><small>Vendas</small><strong>{vendas.length}</strong></div>
        <div className="painel"><small>Para enviar</small><strong>{aEnviar}</strong></div>
      </div>
      {vendas.length === 0 && (
        <div className="vazio">
          <span>💰</span>
          <p>Quando alguém comprar um anúncio seu, a venda aparece aqui.</p>
        </div>
      )}
      {vendas.map((v) => (
        <div key={v.pedidoId} className="painel cartao-venda">
          <div className="secao-titulo">
            <strong>Venda #{v.pedidoId} · {v.comprador}</strong>
            <span className={'status status-' + v.status.toLowerCase()}>{v.enviado && v.status === 'PAGO' ? 'Você já enviou' : v.statusRotulo}</span>
          </div>
          {v.itens.map((i) => <div key={i.produtoId}>{i.quantidade}x {i.titulo} · {moeda(i.subtotal)}</div>)}
          <small className="texto-suave">📍 {v.endereco}</small>
          <small className="texto-suave">Pago em {dataHora(v.pagoEm)}</small>
          <div className="secao-titulo">
            <strong>{moeda(v.total)}</strong>
            {!v.enviado && <button className="botao botao-principal" onClick={() => enviar(v)}>Marcar como enviado</button>}
          </div>
        </div>
      ))}
    </div>
  )
}
