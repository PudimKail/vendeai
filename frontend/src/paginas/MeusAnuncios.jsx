import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { get, post } from '../api'
import { moeda } from '../formato'
import { useSessao } from '../sessao'
import { Estrelas, ImagemProduto } from '../componentes/Produto'

export default function MeusAnuncios() {
  const { avisar } = useSessao()
  const [anuncios, setAnuncios] = useState(null)

  useEffect(() => {
    get('/anuncios').then(setAnuncios)
  }, [])

  const pausarOuAtivar = async (produto) => {
    const atualizado = await post(`/anuncios/${produto.id}/pausar`)
    setAnuncios(anuncios.map((a) => (a.id === atualizado.id ? atualizado : a)))
    avisar(atualizado.ativo ? '▶️ Anúncio ativado' : '⏸️ Anúncio pausado')
  }

  if (!anuncios) return <div className="carregando" />

  return (
    <div className="pagina-media">
      <div className="secao-titulo">
        <h1>Meus anúncios</h1>
        <Link to="/vender" className="botao botao-principal">+ Novo anúncio</Link>
      </div>
      {anuncios.length === 0 && (
        <div className="vazio">
          <span>🏷️</span>
          <p>Você ainda não anunciou nada. Que tal começar agora?</p>
        </div>
      )}
      {anuncios.map((p) => (
        <div key={p.id} className={'painel linha-anuncio' + (p.ativo ? '' : ' pausado')}>
          <Link to={`/produto/${p.id}`}><ImagemProduto produto={p} /></Link>
          <div className="anuncio-info">
            <Link to={`/produto/${p.id}`}><strong>{p.titulo}</strong></Link>
            <span>{moeda(p.preco)} · {p.estoque} em estoque · {p.vendidos} vendidos</span>
            {p.totalAvaliacoes > 0 && <Estrelas nota={p.notaMedia} total={p.totalAvaliacoes} />}
            <span className={'status ' + (p.ativo ? 'status-pago' : 'status-cancelado')}>{p.ativo ? 'Ativo' : 'Pausado'}</span>
          </div>
          <div className="anuncio-acoes">
            <Link to={`/anuncios/${p.id}/editar`} className="botao botao-contorno botao-pequeno">Editar</Link>
            <button className="botao botao-contorno botao-pequeno" onClick={() => pausarOuAtivar(p)}>{p.ativo ? 'Pausar' : 'Ativar'}</button>
          </div>
        </div>
      ))}
    </div>
  )
}
