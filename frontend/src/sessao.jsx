import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { get, post } from './api'

// Guarda quem está logado e os contadores do cabeçalho (carrinho e notificações) para o site inteiro.
const SessaoContexto = createContext(null)

const VAZIA = { usuario: null, itensCarrinho: 0, notificacoesNaoLidas: 0 }
const INTERVALO_NOTIFICACOES_MS = 20000

export function SessaoProvider({ children }) {
  const [sessao, setSessao] = useState(VAZIA)
  const [carregando, setCarregando] = useState(true)
  const [avisos, setAvisos] = useState([])
  const naoLidasAntes = useRef(0)

  const avisar = useCallback((texto, tipo = 'sucesso') => {
    const id = Math.random()
    setAvisos((lista) => [...lista, { id, texto, tipo }])
    setTimeout(() => setAvisos((lista) => lista.filter((a) => a.id !== id)), 4000)
  }, [])

  const atualizar = useCallback((nova) => {
    setSessao({ ...VAZIA, ...nova })
  }, [])

  const recarregar = useCallback(async () => {
    try {
      atualizar(await get('/auth/eu'))
    } finally {
      setCarregando(false)
    }
  }, [atualizar])

  useEffect(() => {
    recarregar()
  }, [recarregar])

  // Confere de tempos em tempos se chegou notificação nova (deixa o sininho "ao vivo").
  useEffect(() => {
    if (!sessao.usuario) return
    const timer = setInterval(async () => {
      const nova = await get('/auth/eu').catch(() => null)
      if (nova) atualizar(nova)
    }, INTERVALO_NOTIFICACOES_MS)
    return () => clearInterval(timer)
  }, [sessao.usuario, atualizar])

  useEffect(() => {
    if (sessao.notificacoesNaoLidas > naoLidasAntes.current && naoLidasAntes.current !== 0) {
      avisar('🔔 Você tem uma notificação nova', 'info')
    }
    naoLidasAntes.current = sessao.notificacoesNaoLidas
  }, [sessao.notificacoesNaoLidas, avisar])

  const entrar = async (email, senha) => atualizar(await post('/auth/login', { email, senha }))

  const cadastrar = async (dados) => {
    const nova = await post('/auth/cadastro', dados)
    atualizar(nova)
    return nova
  }

  const sair = async () => {
    atualizar(await post('/auth/sair'))
    await get('/auth/eu') // pega um token CSRF novo
  }

  const valor = { ...sessao, carregando, atualizar, recarregar, entrar, cadastrar, sair, avisar, avisos }
  return <SessaoContexto.Provider value={valor}>{children}</SessaoContexto.Provider>
}

export const useSessao = () => useContext(SessaoContexto)
