import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useSessao } from '../sessao'

export default function Entrar() {
  const { entrar, avisar } = useSessao()
  const [params] = useSearchParams()
  const navegar = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  const enviar = async (e) => {
    e.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      await entrar(email, senha)
      avisar('👋 Que bom te ver de novo!')
      navegar(params.get('voltar') || '/')
    } catch (falha) {
      setErro(falha.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pagina-estreita">
      <form className="painel formulario" onSubmit={enviar}>
        <h1>Entrar</h1>
        <label>
          E-mail
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" autoFocus />
        </label>
        <label>
          Senha
          <input type="password" value={senha} onChange={(e) => setSenha(e.target.value)} required autoComplete="current-password" />
        </label>
        {erro && <p className="erro">{erro}</p>}
        <button className="botao botao-principal botao-largo" disabled={enviando}>{enviando ? 'Entrando...' : 'Entrar'}</button>
        <p className="texto-centro">Ainda não tem conta? <Link to="/cadastro">Crie agora</Link></p>
        <p className="dica">
          <strong>Contas de demonstração</strong> (senha <code>demo1234</code>):<br />
          comprador@vendeai.dev · loja@vendeai.dev · techstore@vendeai.dev
        </p>
      </form>
    </div>
  )
}
