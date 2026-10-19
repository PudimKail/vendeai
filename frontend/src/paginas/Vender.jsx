import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api, get } from '../api'
import { moeda } from '../formato'
import { useSessao } from '../sessao'

const VAZIO = { titulo: '', descricao: '', preco: '', estoque: 1, condicao: 'NOVO', categoriaId: '', freteGratis: false }

/** Cadastrar produto (/vender) e editar anúncio (/anuncios/:id/editar) usam o mesmo formulário. */
export default function Vender() {
  const { id } = useParams()
  const { usuario, avisar } = useSessao()
  const navegar = useNavigate()
  const [categorias, setCategorias] = useState([])
  const [dados, setDados] = useState(VAZIO)
  const [foto, setFoto] = useState(null)
  const [previa, setPrevia] = useState(null)
  const [erros, setErros] = useState({})
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    get('/categorias').then(setCategorias)
    if (id) {
      get(`/anuncios/${id}`).then((p) => {
        setDados({ titulo: p.titulo, descricao: p.descricao, preco: String(p.preco).replace('.', ','), estoque: p.estoque,
          condicao: p.condicao, categoriaId: p.categoria.id, freteGratis: p.freteGratis })
        setPrevia(p.imagemUrl)
      })
    }
  }, [id])

  useEffect(() => () => previa?.startsWith('blob:') && URL.revokeObjectURL(previa), [previa])

  const campo = (nome) => ({
    value: dados[nome],
    onChange: (e) => setDados({ ...dados, [nome]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }),
  })

  const escolherFoto = (e) => {
    const arquivo = e.target.files[0]
    if (!arquivo) return
    if (arquivo.size > 5 * 1024 * 1024) return avisar('A foto pode ter no máximo 5 MB', 'erro')
    setFoto(arquivo)
    setPrevia(URL.createObjectURL(arquivo))
  }

  const salvar = async (e) => {
    e.preventDefault()
    setErros({})
    setEnviando(true)
    const formulario = new FormData()
    Object.entries(dados).forEach(([k, v]) => formulario.append(k, k === 'preco' ? String(v).replace(/\./g, '').replace(',', '.') : v))
    if (foto) formulario.append('foto', foto)
    try {
      const produto = await api(id ? `/anuncios/${id}` : '/anuncios', { metodo: 'POST', formulario })
      avisar(id ? '✅ Anúncio atualizado' : '🎉 Anúncio publicado!')
      navegar(`/produto/${produto.id}`)
    } catch (falha) {
      if (falha.dados?.campos) setErros(falha.dados.campos)
      else if (falha.dados?.codigo === 'EMAIL_NAO_VERIFICADO') navegar('/verificar-email')
      else avisar(falha.message, 'erro')
    } finally {
      setEnviando(false)
    }
  }

  const precoNumero = Number(String(dados.preco).replace(/\./g, '').replace(',', '.'))

  return (
    <div className="pagina-media">
      <h1>{id ? 'Editar anúncio' : 'O que você vai vender?'}</h1>
      {!id && usuario && !usuario.emailVerificado && (
        <p className="alerta">Confirme seu e-mail antes de publicar. <Link to="/verificar-email">Confirmar agora</Link></p>
      )}
      <form className="vender-layout" onSubmit={salvar} noValidate>
        <div className="painel formulario">
          <Campo rotulo="Título do anúncio" erro={erros.titulo} ajuda="Ex.: Smartphone Galaxy A55 128GB Preto">
            <input {...campo('titulo')} maxLength={120} required />
          </Campo>
          <Campo rotulo="Categoria" erro={erros.categoriaId}>
            <select {...campo('categoriaId')} required>
              <option value="">Escolha...</option>
              {categorias.map((c) => <option key={c.id} value={c.id}>{c.icone} {c.nome}</option>)}
            </select>
          </Campo>
          <div className="grade-campos">
            <Campo rotulo="Preço (R$)" erro={erros.preco}>
              <input {...campo('preco')} inputMode="decimal" placeholder="0,00" required />
            </Campo>
            <Campo rotulo="Estoque" erro={erros.estoque}>
              <input type="number" min="1" {...campo('estoque')} required />
            </Campo>
          </div>
          <fieldset className="opcoes-linha">
            <legend>Condição</legend>
            {[['NOVO', 'Novo'], ['USADO', 'Usado']].map(([v, r]) => (
              <label key={v} className="filtro-opcao">
                <input type="radio" name="condicao" checked={dados.condicao === v} onChange={() => setDados({ ...dados, condicao: v })} /> {r}
              </label>
            ))}
          </fieldset>
          <label className="filtro-opcao">
            <input type="checkbox" checked={dados.freteGratis} onChange={campo('freteGratis').onChange} />
            Oferecer frete grátis (anúncios com frete grátis vendem mais)
          </label>
          <Campo rotulo="Descrição" erro={erros.descricao}>
            <textarea {...campo('descricao')} rows={7} maxLength={4000} placeholder="Conte os detalhes: modelo, tamanho, estado de conservação, o que vem na caixa..." />
          </Campo>
        </div>

        <aside className="painel">
          <h2>Foto</h2>
          <label className="area-foto">
            {previa ? <img src={previa} alt="Prévia da foto" /> : <span>📷<br />Clique para escolher uma foto<br /><small>JPG, PNG ou WEBP até 5 MB</small></span>}
            <input type="file" accept="image/jpeg,image/png,image/webp" onChange={escolherFoto} hidden />
          </label>
          {precoNumero > 0 && (
            <p className="texto-suave">Os compradores vão ver: <strong>{moeda(precoNumero)}</strong> ou 10x de {moeda(precoNumero / 10)}</p>
          )}
          <button className="botao botao-principal botao-largo" disabled={enviando}>
            {enviando ? 'Salvando...' : id ? 'Salvar alterações' : 'Publicar anúncio'}
          </button>
        </aside>
      </form>
    </div>
  )
}

function Campo({ rotulo, erro, ajuda, children }) {
  return (
    <label className={erro ? 'com-erro' : ''}>
      {rotulo}
      {children}
      {erro ? <span className="erro">{erro}</span> : ajuda && <small className="texto-suave">{ajuda}</small>}
    </label>
  )
}
