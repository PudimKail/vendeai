import { useSessao } from '../sessao'

/** Mensagens rápidas no canto da tela ("Adicionado ao carrinho", erros...). */
export default function Avisos() {
  const { avisos } = useSessao()
  return (
    <div className="avisos" aria-live="polite">
      {avisos.map((a) => (
        <div key={a.id} className={`aviso aviso-${a.tipo}`}>{a.texto}</div>
      ))}
    </div>
  )
}
