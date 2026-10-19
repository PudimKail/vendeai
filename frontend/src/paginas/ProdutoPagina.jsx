import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { get, post } from '../api'
import { dataHora, moeda } from '../formato'
import { useSessao } from '../sessao'
import { Estrelas, GradeProdutos, ImagemProduto } from '../componentes/Produto'
import NaoEncontrado from './NaoEncontrado'

export default function ProdutoPagina() {
  const { id } = useParams()
  const { usuario, atualizar, avisar, recarregar } = useSessao()
  const navegar = useNavigate()
  const [dados, setDados] = useState(null)
  const [erro, setErro] = useState(null)
  const [quantidade, setQuantidade] = useState(1)
  const [enviando, setEnviando] = useState(false)

  const carregar = () => get(`/produtos/${id}`).then(setDados).catch(setErro)

  useEffect(() => {
    setDados(null)
    setQuantidade(1)
    carregar()
  }, [id])

  if (erro) return <NaoEncontrado />
  if (!dados) return <div className="carregando" />

  const { produto, avaliacoes, semelhantes, podeAvaliar, meuAnuncio } = dados

  const adicionar = async (irParaCarrinho) => {
    if (!usuario) return navegar(`/entrar?voltar=/produto/${id}`)
    setEnviando(true)
    try {
      await post('/carrinho', { produtoId: produto.id, quantidade })
      await recarregar()
      if (irParaCarrinho) navegar('/carrinho')
      else avisar('✅ Adicionado ao carrinho')
    } catch (e) {
      if (e.dados?.codigo === 'EMAIL_NAO_VERIFICADO') navegar('/verificar-email')
      else avisar(e.message, 'erro')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <>
      <nav className="trilha">
        <Link to="/">Início</Link> › <Link to={`/busca?categoria=${produto.categoria.slug}`}>{produto.categoria.nome}</Link>
      </nav>

      <div className="produto-layout">
        <div className="produto-foto">
          <ImagemProduto produto={produto} grande />
        </div>

        <div className="produto-compra">
          <p className="texto-suave">{produto.condicaoRotulo} | {produto.vendidos} vendidos</p>
          <h1>{produto.titulo}</h1>
          {produto.totalAvaliacoes > 0 && (
            <a href="#avaliacoes" className="link-estrelas">
              {produto.notaMedia.toFixed(1)} <Estrelas nota={produto.notaMedia} total={produto.totalAvaliacoes} />
            </a>
          )}
          <div className="preco preco-grande">{moeda(produto.preco)}</div>
          <p className="parcela">em <strong>{produto.parcelas}x {moeda(produto.valorParcela)} sem juros</strong></p>
          <p className="frete-gratis">{produto.freteGratis ? '🚚 Frete grátis para todo o Brasil' : '🚚 Frete grátis em compras acima de R$ 79'}</p>

          {!produto.ativo && <p className="alerta">Este anúncio está pausado.</p>}

          {meuAnuncio ? (
            <div className="caixa-info">
              Este é o seu anúncio. Estoque: <strong>{produto.estoque}</strong>
              <Link to={`/anuncios/${produto.id}/editar`} className="botao botao-contorno">Editar anúncio</Link>
            </div>
          ) : produto.ativo && produto.estoque > 0 ? (
            <>
              <label className="quantidade">
                Quantidade:{' '}
                <select value={quantidade} onChange={(e) => setQuantidade(Number(e.target.value))}>
                  {Array.from({ length: Math.min(produto.estoque, 10) }).map((_, i) => (
                    <option key={i + 1} value={i + 1}>{i + 1} {i === 0 ? 'unidade' : 'unidades'}</option>
                  ))}
                </select>
                <span className="texto-suave"> ({produto.estoque} disponíveis)</span>
              </label>
              <button className="botao botao-principal botao-largo" disabled={enviando} onClick={() => adicionar(true)}>Comprar agora</button>
              <button className="botao botao-secundario botao-largo" disabled={enviando} onClick={() => adicionar(false)}>Adicionar ao carrinho</button>
            </>
          ) : (
            produto.ativo && <p className="alerta">Produto esgotado</p>
          )}

          <div className="vendedor">
            Vendido por <strong>{produto.vendedor.nome}</strong>
            <p className="texto-suave">🔒 Compra garantida: receba o produto ou devolvemos seu dinheiro.</p>
          </div>
        </div>
      </div>

      <section className="painel">
        <h2>Descrição</h2>
        <p className="descricao">{produto.descricao || 'O vendedor não colocou descrição.'}</p>
      </section>

      <section className="painel" id="avaliacoes">
        <h2>Opiniões sobre o produto</h2>
        {podeAvaliar && <FormAvaliacao produtoId={produto.id} aoAvaliar={() => { carregar(); avisar('⭐ Obrigado pela avaliação!') }} />}
        {avaliacoes.length === 0 && <p className="texto-suave">Este produto ainda não tem avaliações.</p>}
        {avaliacoes.length > 0 && (
          <div className="resumo-avaliacoes">
            <span className="nota-grande">{produto.notaMedia.toFixed(1)}</span>
            <Estrelas nota={produto.notaMedia} />
            <span className="texto-suave">{produto.totalAvaliacoes} avaliação(ões)</span>
          </div>
        )}
        {avaliacoes.map((a) => (
          <article key={a.id} className="avaliacao">
            <Estrelas nota={a.nota} /> <strong>{a.autor}</strong>
            <small className="texto-suave"> · {dataHora(a.criadoEm)}</small>
            {a.comentario && <p>{a.comentario}</p>}
          </article>
        ))}
      </section>

      {semelhantes.length > 0 && (
        <section>
          <h2>Quem viu este produto também viu</h2>
          <GradeProdutos produtos={semelhantes} />
        </section>
      )}
    </>
  )
}

/** Estrelas clicáveis + comentário. Só aparece para quem comprou e recebeu. */
export function FormAvaliacao({ produtoId, aoAvaliar }) {
  const { avisar } = useSessao()
  const [nota, setNota] = useState(0)
  const [passando, setPassando] = useState(0)
  const [comentario, setComentario] = useState('')
  const [enviando, setEnviando] = useState(false)

  const enviar = async (e) => {
    e.preventDefault()
    if (!nota) return avisar('Escolha de 1 a 5 estrelas', 'erro')
    setEnviando(true)
    try {
      await post(`/produtos/${produtoId}/avaliacoes`, { nota, comentario })
      aoAvaliar()
    } catch (erro) {
      avisar(erro.message, 'erro')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <form className="form-avaliacao" onSubmit={enviar}>
      <strong>Você comprou este produto. O que achou?</strong>
      <div className="estrelas-input" onMouseLeave={() => setPassando(0)}>
        {[1, 2, 3, 4, 5].map((n) => (
          <button type="button" key={n} className={n <= (passando || nota) ? 'acesa' : ''}
                  onMouseEnter={() => setPassando(n)} onClick={() => setNota(n)} aria-label={`${n} estrela(s)`}>★</button>
        ))}
      </div>
      <textarea placeholder="Conte para outros compradores (opcional)" value={comentario} maxLength={1000}
                onChange={(e) => setComentario(e.target.value)} />
      <button className="botao botao-principal" disabled={enviando}>Publicar avaliação</button>
    </form>
  )
}
