import { useEffect } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useSessao } from './sessao'
import Cabecalho from './componentes/Cabecalho'
import Rodape from './componentes/Rodape'
import Avisos from './componentes/Avisos'
import Inicio from './paginas/Inicio'
import Busca from './paginas/Busca'
import ProdutoPagina from './paginas/ProdutoPagina'
import Entrar from './paginas/Entrar'
import Cadastro from './paginas/Cadastro'
import VerificarEmail from './paginas/VerificarEmail'
import Carrinho from './paginas/Carrinho'
import Checkout from './paginas/Checkout'
import Pedido from './paginas/Pedido'
import MinhasCompras from './paginas/MinhasCompras'
import Vender from './paginas/Vender'
import MeusAnuncios from './paginas/MeusAnuncios'
import MinhasVendas from './paginas/MinhasVendas'
import Notificacoes from './paginas/Notificacoes'
import NaoEncontrado from './paginas/NaoEncontrado'

/** Páginas que só abrem com login. Sem login, vai para "Entrar" e depois volta. */
function Privada({ children }) {
  const { usuario, carregando } = useSessao()
  const local = useLocation()
  if (carregando) return <div className="carregando" />
  if (!usuario) return <Navigate to={`/entrar?voltar=${encodeURIComponent(local.pathname)}`} replace />
  return children
}

export default function App() {
  const { pathname } = useLocation()
  // Chaves {} de propósito: o efeito não pode devolver o retorno de scrollTo (em navegadores novos é uma Promise)
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return (
    <>
      <Cabecalho />
      <main className="conteudo">
        <Routes>
          <Route path="/" element={<Inicio />} />
          <Route path="/busca" element={<Busca />} />
          <Route path="/produto/:id" element={<ProdutoPagina />} />
          <Route path="/entrar" element={<Entrar />} />
          <Route path="/cadastro" element={<Cadastro />} />
          <Route path="/verificar-email" element={<Privada><VerificarEmail /></Privada>} />
          <Route path="/carrinho" element={<Privada><Carrinho /></Privada>} />
          <Route path="/checkout" element={<Privada><Checkout /></Privada>} />
          <Route path="/compras" element={<Privada><MinhasCompras /></Privada>} />
          <Route path="/compras/:id" element={<Privada><Pedido /></Privada>} />
          <Route path="/vender" element={<Privada><Vender /></Privada>} />
          <Route path="/anuncios" element={<Privada><MeusAnuncios /></Privada>} />
          <Route path="/anuncios/:id/editar" element={<Privada><Vender /></Privada>} />
          <Route path="/vendas" element={<Privada><MinhasVendas /></Privada>} />
          <Route path="/notificacoes" element={<Privada><Notificacoes /></Privada>} />
          <Route path="*" element={<NaoEncontrado />} />
        </Routes>
      </main>
      <Rodape />
      <Avisos />
    </>
  )
}
