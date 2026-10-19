import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useNavigate, useSearchParams } from 'react-router-dom'
import { get, post } from '../api'
import { haQuanto } from '../formato'
import { useSessao } from '../sessao'

export default function Cabecalho() {
  const { usuario, itensCarrinho, notificacoesNaoLidas, sair, recarregar } = useSessao()
  const [params] = useSearchParams()
  const [busca, setBusca] = useState(params.get('q') ?? '')
  const [categorias, setCategorias] = useState([])
  const [menu, setMenu] = useState(null) // 'conta' | 'notificacoes' | null
  const [notificacoes, setNotificacoes] = useState([])
  const navegar = useNavigate()
  const areaMenus = useRef(null)

  useEffect(() => {
    get('/categorias').then(setCategorias).catch(() => {})
  }, [])

  useEffect(() => {
    setBusca(params.get('q') ?? '')
  }, [params])

  // Fecha o menu aberto ao clicar fora dele
  useEffect(() => {
    const fechar = (e) => areaMenus.current && !areaMenus.current.contains(e.target) && setMenu(null)
    document.addEventListener('mousedown', fechar)
    return () => document.removeEventListener('mousedown', fechar)
  }, [])

  const buscar = (e) => {
    e.preventDefault()
    navegar(busca.trim() ? `/busca?q=${encodeURIComponent(busca.trim())}` : '/busca')
  }

  const abrirNotificacoes = async () => {
    if (menu === 'notificacoes') return setMenu(null)
    setMenu('notificacoes')
    setNotificacoes(await get('/notificacoes'))
    if (notificacoesNaoLidas > 0) {
      await post('/notificacoes/lidas')
      recarregar()
    }
  }

  const fazerLogout = async () => {
    setMenu(null)
    await sair()
    navegar('/')
  }

  return (
    <header className="cabecalho">
      <div className="cabecalho-linha">
        <Link to="/" className="logo" aria-label="VendeAí, página inicial">
          <span className="logo-icone">🛍️</span> Vende<strong>Aí</strong>
        </Link>

        <form className="busca" onSubmit={buscar} role="search">
          <input
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            placeholder="Buscar produtos, marcas e muito mais..."
            aria-label="Buscar produtos"
          />
          <button type="submit" aria-label="Buscar">🔍</button>
        </form>

        <nav className="acoes" ref={areaMenus}>
          <Link to="/vender" className="botao-vender">+ Vender</Link>

          {usuario ? (
            <>
              <div className="menu-wrap">
                <button className="icone-botao" onClick={abrirNotificacoes} aria-label="Notificações">
                  🔔{notificacoesNaoLidas > 0 && <span className="contador">{notificacoesNaoLidas}</span>}
                </button>
                {menu === 'notificacoes' && (
                  <div className="menu menu-notificacoes">
                    <div className="menu-titulo">Notificações</div>
                    {notificacoes.length === 0 && <p className="menu-vazio">Nada por aqui ainda.</p>}
                    {notificacoes.slice(0, 6).map((n) => (
                      <Link key={n.id} to={n.link || '/'} className={'notificacao' + (n.lida ? '' : ' nova')} onClick={() => setMenu(null)}>
                        <span className="notificacao-icone">{n.icone}</span>
                        <span>
                          {n.mensagem}
                          <small>{haQuanto(n.criadoEm)}</small>
                        </span>
                      </Link>
                    ))}
                    <Link to="/notificacoes" className="menu-rodape" onClick={() => setMenu(null)}>Ver todas</Link>
                  </div>
                )}
              </div>

              <div className="menu-wrap">
                <button className="conta-botao" onClick={() => setMenu(menu === 'conta' ? null : 'conta')}>
                  <span className="avatar">{usuario.primeiroNome[0]}</span>
                  <span className="so-desktop">Olá, {usuario.primeiroNome}</span> ▾
                </button>
                {menu === 'conta' && (
                  <div className="menu" onClick={() => setMenu(null)}>
                    <Link to="/compras">🧾 Minhas compras</Link>
                    <Link to="/anuncios">🏷️ Meus anúncios</Link>
                    <Link to="/vendas">💰 Minhas vendas</Link>
                    <Link to="/notificacoes">🔔 Notificações</Link>
                    <button onClick={fazerLogout}>🚪 Sair</button>
                  </div>
                )}
              </div>
            </>
          ) : (
            <>
              <Link to="/cadastro" className="link-claro so-desktop">Crie sua conta</Link>
              <Link to="/entrar" className="link-claro">Entrar</Link>
            </>
          )}

          <Link to="/carrinho" className="icone-botao" aria-label="Carrinho">
            🛒{itensCarrinho > 0 && <span className="contador">{itensCarrinho}</span>}
          </Link>
        </nav>
      </div>

      <nav className="categorias-barra" aria-label="Categorias">
        {categorias.map((c) => (
          <NavLink key={c.slug} to={`/busca?categoria=${c.slug}`}>
            {c.icone} {c.nome}
          </NavLink>
        ))}
      </nav>

      {usuario && !usuario.emailVerificado && (
        <div className="faixa-verificacao">
          ✉️ Confirme seu e-mail para comprar e vender. <Link to="/verificar-email">Confirmar agora</Link>
        </div>
      )}
    </header>
  )
}
