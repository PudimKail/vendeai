import { Link } from 'react-router-dom'
import { moeda } from '../formato'

/** Foto do produto. Sem foto, mostra o ícone da categoria num fundo colorido. */
export function ImagemProduto({ produto, grande = false }) {
  const classe = 'imagem-produto' + (grande ? ' grande' : '')
  if (produto.imagemUrl) {
    return <img className={classe} src={produto.imagemUrl} alt={produto.titulo} loading="lazy" />
  }
  const tom = (produto.id * 47) % 360
  return (
    <div className={classe + ' sem-foto'} style={{ '--tom': tom }} role="img" aria-label={produto.titulo}>
      <span>{produto.categoria.icone}</span>
    </div>
  )
}

export function Estrelas({ nota, total }) {
  const cheias = Math.round(nota)
  return (
    <span className="estrelas" title={`${nota.toFixed(1)} de 5`}>
      {'★'.repeat(cheias)}
      <span className="estrelas-vazias">{'★'.repeat(5 - cheias)}</span>
      {total !== undefined && <small> ({total})</small>}
    </span>
  )
}

export function CartaoProduto({ produto }) {
  return (
    <Link to={`/produto/${produto.id}`} className="cartao-produto">
      <ImagemProduto produto={produto} />
      <div className="cartao-info">
        <h3>{produto.titulo}</h3>
        {produto.totalAvaliacoes > 0 && <Estrelas nota={produto.notaMedia} total={produto.totalAvaliacoes} />}
        <div className="preco">{moeda(produto.preco)}</div>
        <div className="parcela">em {produto.parcelas}x {moeda(produto.valorParcela)} sem juros</div>
        {produto.freteGratis && <div className="frete-gratis">Frete grátis</div>}
        {produto.condicao === 'USADO' && <span className="etiqueta">Usado</span>}
      </div>
    </Link>
  )
}

export function GradeProdutos({ produtos }) {
  return (
    <div className="grade-produtos">
      {produtos.map((p) => <CartaoProduto key={p.id} produto={p} />)}
    </div>
  )
}

export function Carregando() {
  return (
    <div className="grade-produtos">
      {Array.from({ length: 8 }).map((_, i) => <div key={i} className="cartao-produto esqueleto" />)}
    </div>
  )
}
