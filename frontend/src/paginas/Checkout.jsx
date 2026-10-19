import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { get, post } from '../api'
import { moeda } from '../formato'
import { useSessao } from '../sessao'

const ENDERECO_VAZIO = { cep: '', rua: '', numero: '', complemento: '', bairro: '', cidade: '', uf: '' }

export default function Checkout() {
  const { recarregar, avisar } = useSessao()
  const navegar = useNavigate()
  const [carrinho, setCarrinho] = useState(null)
  const [endereco, setEndereco] = useState(ENDERECO_VAZIO)
  const [forma, setForma] = useState('PIX')
  const [buscandoCep, setBuscandoCep] = useState(false)
  const [erros, setErros] = useState({})
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    get('/carrinho').then(setCarrinho)
  }, [])

  if (carrinho && carrinho.itens.length === 0) return <Navigate to="/carrinho" replace />

  const campo = (nome) => ({
    value: endereco[nome],
    onChange: (e) => setEndereco({ ...endereco, [nome]: e.target.value }),
  })

  /** Ao digitar os 8 números do CEP, completa rua, bairro, cidade e UF pelo ViaCEP (serviço público e grátis). */
  const mudarCep = async (e) => {
    const cep = e.target.value.replace(/\D/g, '').slice(0, 8)
    const formatado = cep.length > 5 ? `${cep.slice(0, 5)}-${cep.slice(5)}` : cep
    setEndereco((atual) => ({ ...atual, cep: formatado }))
    if (cep.length !== 8) return
    setBuscandoCep(true)
    try {
      const r = await fetch(`https://viacep.com.br/ws/${cep}/json/`).then((res) => res.json())
      if (r.erro) {
        setErros({ cep: 'CEP não encontrado' })
      } else {
        setErros({})
        setEndereco((atual) => ({ ...atual, rua: r.logradouro || atual.rua, bairro: r.bairro || atual.bairro, cidade: r.localidade, uf: r.uf }))
      }
    } catch {
      // Sem internet ou ViaCEP fora do ar: a pessoa preenche à mão
    } finally {
      setBuscandoCep(false)
    }
  }

  const finalizar = async (e) => {
    e.preventDefault()
    setErros({})
    setEnviando(true)
    try {
      const pedido = await post('/pedidos', { endereco, formaPagamento: forma })
      await recarregar()
      navegar(`/compras/${pedido.id}`)
    } catch (falha) {
      if (falha.dados?.campos) {
        setErros(Object.fromEntries(Object.entries(falha.dados.campos).map(([k, v]) => [k.replace('endereco.', ''), v])))
      } else if (falha.dados?.codigo === 'EMAIL_NAO_VERIFICADO') {
        navegar('/verificar-email')
      } else {
        avisar(falha.message, 'erro')
      }
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="carrinho-layout" onSubmit={finalizar}>
      <section>
        <div className="painel formulario">
          <h1>Endereço de entrega</h1>
          <div className="grade-campos">
            <Campo rotulo={buscandoCep ? 'CEP (buscando...)' : 'CEP'} erro={erros.cep}>
              <input {...campo('cep')} onChange={mudarCep} placeholder="00000-000" inputMode="numeric" required />
            </Campo>
            <Campo rotulo="Rua / Avenida" erro={erros.rua} largo><input {...campo('rua')} required /></Campo>
            <Campo rotulo="Número" erro={erros.numero}><input {...campo('numero')} required /></Campo>
            <Campo rotulo="Complemento"><input {...campo('complemento')} placeholder="Apto, bloco..." /></Campo>
            <Campo rotulo="Bairro" erro={erros.bairro}><input {...campo('bairro')} required /></Campo>
            <Campo rotulo="Cidade" erro={erros.cidade}><input {...campo('cidade')} required /></Campo>
            <Campo rotulo="UF" erro={erros.uf}><input {...campo('uf')} maxLength={2} required /></Campo>
          </div>
        </div>

        <div className="painel">
          <h2>Forma de pagamento</h2>
          <label className={'opcao-pagamento' + (forma === 'PIX' ? ' selecionada' : '')}>
            <input type="radio" name="forma" checked={forma === 'PIX'} onChange={() => setForma('PIX')} />
            <span className="opcao-icone">⚡</span>
            <span><strong>Pix</strong><small>Aprovação na hora</small></span>
          </label>
          <label className={'opcao-pagamento' + (forma === 'CARTAO' ? ' selecionada' : '')}>
            <input type="radio" name="forma" checked={forma === 'CARTAO'} onChange={() => setForma('CARTAO')} />
            <span className="opcao-icone">💳</span>
            <span><strong>Cartão de crédito</strong><small>Em até 10x sem juros</small></span>
          </label>
        </div>
      </section>

      <aside className="painel resumo">
        <h2>Resumo</h2>
        {!carrinho && <div className="carregando" />}
        {carrinho && (
          <>
            {carrinho.itens.map((i) => (
              <div key={i.id} className="linha linha-item"><span>{i.quantidade}x {i.produto.titulo}</span><span>{moeda(i.subtotal)}</span></div>
            ))}
            <div className="linha"><span>Frete</span><span>{carrinho.frete > 0 ? moeda(carrinho.frete) : 'Grátis'}</span></div>
            <div className="linha total"><span>Total</span><span>{moeda(carrinho.total)}</span></div>
          </>
        )}
        <button className="botao botao-principal botao-largo" disabled={enviando || !carrinho}>
          {enviando ? 'Finalizando...' : 'Finalizar compra'}
        </button>
      </aside>
    </form>
  )
}

function Campo({ rotulo, erro, largo, children }) {
  return (
    <label className={(erro ? 'com-erro ' : '') + (largo ? 'campo-largo' : '')}>
      {rotulo}
      {children}
      {erro && <span className="erro">{erro}</span>}
    </label>
  )
}
