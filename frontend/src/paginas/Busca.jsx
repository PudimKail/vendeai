import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { get } from '../api'
import { Carregando, GradeProdutos } from '../componentes/Produto'

const ORDENS = {
  relevancia: 'Mais relevantes',
  'menor-preco': 'Menor preço',
  'maior-preco': 'Maior preço',
  recentes: 'Mais recentes',
}

/** Os filtros ficam na URL (?q=...&categoria=...), então dá para compartilhar o link da busca. */
export default function Busca() {
  const [params, setParams] = useSearchParams()
  const [resultado, setResultado] = useState(null)
  const [categorias, setCategorias] = useState([])
  const [precoMin, setPrecoMin] = useState(params.get('min') ?? '')
  const [precoMax, setPrecoMax] = useState(params.get('max') ?? '')
  const [filtrosAbertos, setFiltrosAbertos] = useState(false)

  useEffect(() => {
    get('/categorias').then(setCategorias)
  }, [])

  useEffect(() => {
    setResultado(null)
    get('/produtos?' + params.toString()).then(setResultado)
  }, [params])

  /** Troca um filtro e volta para a página 1. */
  const filtrar = (nome, valor) => {
    const novos = new URLSearchParams(params)
    if (valor === null || valor === '' || valor === false) novos.delete(nome)
    else novos.set(nome, valor)
    if (nome !== 'pagina') novos.delete('pagina')
    setParams(novos)
  }

  const aplicarPreco = (e) => {
    e.preventDefault()
    const novos = new URLSearchParams(params)
    precoMin ? novos.set('min', precoMin) : novos.delete('min')
    precoMax ? novos.set('max', precoMax) : novos.delete('max')
    novos.delete('pagina')
    setParams(novos)
  }

  const q = params.get('q')
  const categoriaAtual = categorias.find((c) => c.slug === params.get('categoria'))
  const titulo = q ? `"${q}"` : categoriaAtual ? categoriaAtual.nome : 'Todos os produtos'
  const temFiltros = [...params.keys()].some((k) => k !== 'ordem' && k !== 'pagina')

  return (
    <div className="busca-layout">
      <aside className={'filtros' + (filtrosAbertos ? ' aberto' : '')}>
        <h1 className="busca-titulo">{titulo}</h1>
        {resultado && <p className="texto-suave">{resultado.totalItens} resultado(s)</p>}

        <div className="filtro-grupo">
          <h3>Categorias</h3>
          {categorias.map((c) => (
            <button key={c.slug} className={'filtro-link' + (c.slug === params.get('categoria') ? ' ativo' : '')}
                    onClick={() => filtrar('categoria', c.slug === params.get('categoria') ? null : c.slug)}>
              {c.icone} {c.nome}
            </button>
          ))}
        </div>

        <div className="filtro-grupo">
          <h3>Condição</h3>
          {[['', 'Todas'], ['NOVO', 'Novo'], ['USADO', 'Usado']].map(([valor, rotulo]) => (
            <label key={rotulo} className="filtro-opcao">
              <input type="radio" name="condicao" checked={(params.get('condicao') ?? '') === valor}
                     onChange={() => filtrar('condicao', valor)} />
              {rotulo}
            </label>
          ))}
        </div>

        <div className="filtro-grupo">
          <h3>Frete</h3>
          <label className="filtro-opcao">
            <input type="checkbox" checked={params.get('freteGratis') === 'true'}
                   onChange={(e) => filtrar('freteGratis', e.target.checked ? 'true' : null)} />
            Frete grátis
          </label>
        </div>

        <form className="filtro-grupo filtro-preco" onSubmit={aplicarPreco}>
          <h3>Preço</h3>
          <div>
            <input type="number" min="0" placeholder="Mínimo" value={precoMin} onChange={(e) => setPrecoMin(e.target.value)} />
            <span>-</span>
            <input type="number" min="0" placeholder="Máximo" value={precoMax} onChange={(e) => setPrecoMax(e.target.value)} />
            <button className="botao botao-pequeno" aria-label="Aplicar preço">›</button>
          </div>
        </form>

        {temFiltros && (
          <button className="botao-link" onClick={() => { setPrecoMin(''); setPrecoMax(''); setParams({}) }}>
            Limpar filtros
          </button>
        )}
      </aside>

      <section className="resultados">
        <div className="resultados-topo">
          <button className="botao botao-contorno so-celular" onClick={() => setFiltrosAbertos(!filtrosAbertos)}>
            {filtrosAbertos ? 'Fechar filtros' : 'Filtrar'}
          </button>
          <label>
            Ordenar por{' '}
            <select value={params.get('ordem') ?? 'relevancia'} onChange={(e) => filtrar('ordem', e.target.value)}>
              {Object.entries(ORDENS).map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}
            </select>
          </label>
        </div>

        {!resultado && <Carregando />}
        {resultado && resultado.itens.length === 0 && (
          <div className="vazio">
            <span>🔎</span>
            <h2>Não encontramos nada</h2>
            <p>Confira a ortografia ou use palavras mais gerais.</p>
          </div>
        )}
        {resultado && <GradeProdutos produtos={resultado.itens} />}

        {resultado && resultado.totalPaginas > 1 && (
          <nav className="paginacao" aria-label="Páginas">
            <button disabled={resultado.pagina === 0} onClick={() => filtrar('pagina', resultado.pagina - 1)}>‹ Anterior</button>
            {Array.from({ length: resultado.totalPaginas }).map((_, i) => (
              <button key={i} className={i === resultado.pagina ? 'ativo' : ''} onClick={() => filtrar('pagina', i)}>{i + 1}</button>
            ))}
            <button disabled={resultado.pagina + 1 >= resultado.totalPaginas} onClick={() => filtrar('pagina', resultado.pagina + 1)}>Próxima ›</button>
          </nav>
        )}
      </section>
    </div>
  )
}
