import { Link } from 'react-router-dom'

export default function NaoEncontrado() {
  return (
    <div className="vazio">
      <span>🧐</span>
      <h1>Página não encontrada</h1>
      <p>O link pode estar errado ou o anúncio foi removido.</p>
      <Link to="/" className="botao botao-principal">Voltar ao início</Link>
    </div>
  )
}
