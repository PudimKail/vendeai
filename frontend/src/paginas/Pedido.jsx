import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { get, post } from '../api'
import { dataHora, moeda } from '../formato'
import { useSessao } from '../sessao'
import { FormAvaliacao } from './ProdutoPagina'
import NaoEncontrado from './NaoEncontrado'

const ETAPAS = [
  ['AGUARDANDO_PAGAMENTO', 'Pedido feito'],
  ['PAGO', 'Pagamento aprovado'],
  ['ENVIADO', 'A caminho'],
  ['ENTREGUE', 'Entregue'],
]

export default function Pedido() {
  const { id } = useParams()
  const { recarregar, avisar } = useSessao()
  const [pedido, setPedido] = useState(null)
  const [erro, setErro] = useState(false)
  const [avaliando, setAvaliando] = useState(null)

  const carregar = () => get(`/pedidos/${id}`).then(setPedido).catch(() => setErro(true))

  useEffect(() => {
    carregar()
  }, [id])

  const acao = async (caminho, mensagem) => {
    try {
      setPedido(await post(`/pedidos/${id}/${caminho}`))
      recarregar()
      avisar(mensagem)
    } catch (e) {
      avisar(e.message, 'erro')
    }
  }

  if (erro) return <NaoEncontrado />
  if (!pedido) return <div className="carregando" />

  const etapaAtual = ETAPAS.findIndex(([s]) => s === pedido.status)
  const cancelado = pedido.status === 'CANCELADO'

  return (
    <div className="pedido-layout">
      <div className="painel">
        <div className="secao-titulo">
          <h1>Pedido #{pedido.id}</h1>
          <span className={'status status-' + pedido.status.toLowerCase()}>{pedido.statusRotulo}</span>
        </div>
        <p className="texto-suave">Feito em {dataHora(pedido.criadoEm)}</p>

        {!cancelado && (
          <ol className="linha-do-tempo">
            {ETAPAS.map(([status, rotulo], i) => (
              <li key={status} className={i <= etapaAtual ? 'feito' : ''}>{rotulo}</li>
            ))}
          </ol>
        )}

        {pedido.status === 'AGUARDANDO_PAGAMENTO' && pedido.formaPagamento === 'PIX' && (
          <PagamentoPix pedido={pedido} aoPagar={() => acao('pix', '✅ Pagamento aprovado!')} />
        )}
        {pedido.status === 'AGUARDANDO_PAGAMENTO' && pedido.formaPagamento === 'CARTAO' && (
          <PagamentoCartao pedido={pedido} aoPagar={(p) => { setPedido(p); recarregar(); avisar('✅ Pagamento aprovado!') }} />
        )}
        {pedido.status === 'AGUARDANDO_PAGAMENTO' && (
          <button className="botao-link" onClick={() => acao('cancelar', 'Pedido cancelado')}>Cancelar pedido</button>
        )}

        {pedido.status === 'PAGO' && <p className="caixa-info">📦 O vendedor está preparando o envio. Você vai receber uma notificação quando sair.</p>}
        {pedido.status === 'ENVIADO' && (
          <div className="caixa-info">
            🚚 Seu pedido está a caminho!
            <button className="botao botao-principal" onClick={() => acao('recebido', '📦 Recebimento confirmado. Avalie seus produtos!')}>
              Já recebi
            </button>
          </div>
        )}
      </div>

      <div className="painel">
        <h2>Produtos</h2>
        {pedido.itens.map((item) => (
          <div key={item.produtoId} className="item-pedido">
            <div>
              <Link to={`/produto/${item.produtoId}`}>{item.quantidade}x {item.titulo}</Link>
              <small className="texto-suave">Vendido por {item.vendedor} {item.enviado && '· enviado'}</small>
            </div>
            <strong>{moeda(item.subtotal)}</strong>
            {pedido.status === 'ENTREGUE' && !item.jaAvaliado && avaliando !== item.produtoId && (
              <button className="botao botao-contorno botao-pequeno" onClick={() => setAvaliando(item.produtoId)}>⭐ Avaliar</button>
            )}
            {item.jaAvaliado && <small className="frete-gratis">Avaliado ✓</small>}
            {avaliando === item.produtoId && (
              <div className="item-avaliacao">
                <FormAvaliacao produtoId={item.produtoId} aoAvaliar={() => { setAvaliando(null); carregar(); avisar('⭐ Obrigado pela avaliação!') }} />
              </div>
            )}
          </div>
        ))}
        <div className="linha"><span>Produtos</span><span>{moeda(pedido.subtotal)}</span></div>
        <div className="linha"><span>Frete</span><span>{pedido.frete > 0 ? moeda(pedido.frete) : 'Grátis'}</span></div>
        <div className="linha total"><span>Total</span><span>{moeda(pedido.total)}</span></div>
        <p className="texto-suave">
          {pedido.formaPagamentoRotulo}
          {pedido.cartaoFinal && ` final ${pedido.cartaoFinal} em ${pedido.parcelas}x`}
        </p>
        <h3>Entrega</h3>
        <p>{pedido.endereco.rua}, {pedido.endereco.numero} {pedido.endereco.complemento} - {pedido.endereco.bairro}, {pedido.endereco.cidade}/{pedido.endereco.uf?.toUpperCase()}</p>
      </div>
    </div>
  )
}

function PagamentoPix({ pedido, aoPagar }) {
  const [copiado, setCopiado] = useState(false)
  const copiar = async () => {
    await navigator.clipboard.writeText(pedido.codigoPix)
    setCopiado(true)
    setTimeout(() => setCopiado(false), 2000)
  }
  return (
    <div className="pagamento">
      <h2>Pague com Pix</h2>
      <p>Abra o app do seu banco e escaneie o QR Code ou use o código "copia e cola".</p>
      <img src={pedido.pixQrCode} alt="QR Code do Pix" className="qr-code" />
      <div className="copia-cola">
        <input readOnly value={pedido.codigoPix} aria-label="Código Pix copia e cola" />
        <button className="botao botao-secundario" onClick={copiar}>{copiado ? 'Copiado!' : 'Copiar'}</button>
      </div>
      <p className="dica">
        <strong>Modo demonstração:</strong> nenhum valor é cobrado. Clique abaixo para simular a confirmação do banco.
      </p>
      <button className="botao botao-principal" onClick={aoPagar}>Já paguei (simular)</button>
    </div>
  )
}

function PagamentoCartao({ pedido, aoPagar }) {
  const [cartao, setCartao] = useState({ numero: '', nome: '', validade: '', cvv: '', parcelas: 1 })
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  const formatarNumero = (v) => v.replace(/\D/g, '').slice(0, 19).replace(/(\d{4})(?=\d)/g, '$1 ')
  const formatarValidade = (v) => v.replace(/\D/g, '').slice(0, 4).replace(/(\d{2})(\d)/, '$1/$2')

  const pagar = async (e) => {
    e.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      aoPagar(await post(`/pedidos/${pedido.id}/cartao`, cartao))
    } catch (falha) {
      setErro(falha.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="pagamento formulario" onSubmit={pagar}>
      <h2>Pague com cartão de crédito</h2>
      <label>
        Número do cartão
        <input value={cartao.numero} inputMode="numeric" autoComplete="cc-number" placeholder="0000 0000 0000 0000" required
               onChange={(e) => setCartao({ ...cartao, numero: formatarNumero(e.target.value) })} />
      </label>
      <label>
        Nome impresso no cartão
        <input value={cartao.nome} autoComplete="cc-name" required onChange={(e) => setCartao({ ...cartao, nome: e.target.value.toUpperCase() })} />
      </label>
      <div className="grade-campos">
        <label>
          Validade
          <input value={cartao.validade} placeholder="MM/AA" inputMode="numeric" autoComplete="cc-exp" required
                 onChange={(e) => setCartao({ ...cartao, validade: formatarValidade(e.target.value) })} />
        </label>
        <label>
          CVV
          <input value={cartao.cvv} inputMode="numeric" maxLength={4} autoComplete="cc-csc" required
                 onChange={(e) => setCartao({ ...cartao, cvv: e.target.value.replace(/\D/g, '') })} />
        </label>
      </div>
      <label>
        Parcelas
        <select value={cartao.parcelas} onChange={(e) => setCartao({ ...cartao, parcelas: Number(e.target.value) })}>
          {Array.from({ length: 10 }).map((_, i) => (
            <option key={i + 1} value={i + 1}>{i + 1}x de {moeda(pedido.total / (i + 1))} sem juros</option>
          ))}
        </select>
      </label>
      {erro && <p className="erro">{erro}</p>}
      <p className="dica">
        <strong>Modo demonstração</strong> (nada é cobrado). Cartões de teste:<br />
        Aprovado: <code>4111 1111 1111 1111</code> · Recusado: <code>4000 0000 0000 0002</code><br />
        Use qualquer validade futura e qualquer CVV.
      </p>
      <button className="botao botao-principal botao-largo" disabled={enviando}>
        {enviando ? 'Processando...' : `Pagar ${moeda(pedido.total)}`}
      </button>
    </form>
  )
}
