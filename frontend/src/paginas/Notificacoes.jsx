import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { get, post } from '../api'
import { haQuanto } from '../formato'
import { useSessao } from '../sessao'

export default function Notificacoes() {
  const { recarregar } = useSessao()
  const [lista, setLista] = useState(null)

  useEffect(() => {
    get('/notificacoes').then((n) => {
      setLista(n)
      if (n.some((x) => !x.lida)) post('/notificacoes/lidas').then(recarregar)
    })
  }, [])

  if (!lista) return <div className="carregando" />

  return (
    <div className="pagina-media">
      <h1>Notificações</h1>
      {lista.length === 0 && <div className="vazio"><span>🔔</span><p>Você não tem notificações.</p></div>}
      <div className="painel lista-notificacoes">
        {lista.map((n) => (
          <Link key={n.id} to={n.link || '/'} className={'notificacao' + (n.lida ? '' : ' nova')}>
            <span className="notificacao-icone">{n.icone}</span>
            <span>
              {n.mensagem}
              <small>{haQuanto(n.criadoEm)}</small>
            </span>
          </Link>
        ))}
      </div>
    </div>
  )
}
