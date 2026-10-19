import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useSessao } from '../sessao'

const VAZIO = { nome: '', email: '', telefone: '', senha: '', confirmacaoSenha: '' }

export default function Cadastro() {
  const { cadastrar } = useSessao()
  const navegar = useNavigate()
  const [dados, setDados] = useState(VAZIO)
  const [erros, setErros] = useState({})
  const [erroGeral, setErroGeral] = useState('')
  const [enviando, setEnviando] = useState(false)

  const campo = (nome) => ({
    value: dados[nome],
    onChange: (e) => setDados({ ...dados, [nome]: e.target.value }),
  })

  const enviar = async (e) => {
    e.preventDefault()
    setErros({})
    setErroGeral('')
    if (dados.senha !== dados.confirmacaoSenha) {
      return setErros({ confirmacaoSenha: 'As senhas não são iguais' })
    }
    setEnviando(true)
    try {
      const sessao = await cadastrar(dados)
      // Leva para a tela do código. No modo demonstração, o código vai junto para aparecer na tela.
      navegar('/verificar-email', { state: { codigoDemo: sessao.codigoDemo } })
    } catch (falha) {
      if (falha.dados?.campos) setErros(falha.dados.campos)
      else setErroGeral(falha.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pagina-estreita">
      <form className="painel formulario" onSubmit={enviar} noValidate>
        <h1>Crie sua conta</h1>
        <p className="texto-suave">É grátis e leva menos de um minuto.</p>
        <Campo rotulo="Nome completo" erro={erros.nome}>
          <input {...campo('nome')} required autoComplete="name" autoFocus />
        </Campo>
        <Campo rotulo="E-mail" erro={erros.email} ajuda="Vamos enviar um código para confirmar.">
          <input type="email" {...campo('email')} required autoComplete="email" />
        </Campo>
        <Campo rotulo="Celular (opcional)" erro={erros.telefone}>
          <input type="tel" {...campo('telefone')} placeholder="(65) 99999-0000" autoComplete="tel" />
        </Campo>
        <Campo rotulo="Senha" erro={erros.senha} ajuda="Pelo menos 6 caracteres.">
          <input type="password" {...campo('senha')} required autoComplete="new-password" />
        </Campo>
        <Campo rotulo="Confirme a senha" erro={erros.confirmacaoSenha}>
          <input type="password" {...campo('confirmacaoSenha')} required autoComplete="new-password" />
        </Campo>
        {erroGeral && <p className="erro">{erroGeral}</p>}
        <button className="botao botao-principal botao-largo" disabled={enviando}>{enviando ? 'Criando...' : 'Criar conta'}</button>
        <p className="texto-centro">Já tem conta? <Link to="/entrar">Entrar</Link></p>
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
