import { useEffect, useRef, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { post } from '../api'
import { useSessao } from '../sessao'

const DIGITOS = 6

/** Seis caixinhas para o código. Cola o código inteiro de uma vez também funciona. */
export default function VerificarEmail() {
  const { usuario, atualizar, avisar } = useSessao()
  const local = useLocation()
  const navegar = useNavigate()
  const [codigo, setCodigo] = useState(Array(DIGITOS).fill(''))
  const [codigoDemo, setCodigoDemo] = useState(local.state?.codigoDemo ?? null)
  const [erro, setErro] = useState('')
  const [espera, setEspera] = useState(60)
  const caixas = useRef([])

  useEffect(() => {
    if (espera <= 0) return
    const t = setTimeout(() => setEspera(espera - 1), 1000)
    return () => clearTimeout(t)
  }, [espera])

  if (usuario?.emailVerificado) return <Navigate to="/" replace />

  const confirmar = async (digitos) => {
    setErro('')
    try {
      atualizar(await post('/auth/verificar-email', { codigo: digitos.join('') }))
      avisar('✉️ E-mail confirmado! Boas compras.')
      navegar('/')
    } catch (falha) {
      setErro(falha.message)
      setCodigo(Array(DIGITOS).fill(''))
      caixas.current[0]?.focus()
    }
  }

  const digitar = (i, valor) => {
    const numeros = valor.replace(/\D/g, '')
    if (!numeros) return setCodigo(codigo.map((d, j) => (j === i ? '' : d)))
    const novo = [...codigo]
    numeros.slice(0, DIGITOS - i).split('').forEach((n, k) => (novo[i + k] = n))
    setCodigo(novo)
    caixas.current[Math.min(i + numeros.length, DIGITOS - 1)]?.focus()
    if (novo.every(Boolean)) confirmar(novo)
  }

  const apagar = (i, e) => {
    if (e.key === 'Backspace' && !codigo[i] && i > 0) caixas.current[i - 1]?.focus()
  }

  const reenviar = async () => {
    try {
      const sessao = await post('/auth/reenviar-codigo')
      setCodigoDemo(sessao.codigoDemo)
      setEspera(60)
      avisar('📨 Enviamos um novo código')
    } catch (falha) {
      avisar(falha.message, 'erro')
    }
  }

  return (
    <div className="pagina-estreita">
      <div className="painel formulario texto-centro">
        <span className="icone-grande">✉️</span>
        <h1>Confirme seu e-mail</h1>
        <p>Enviamos um código de 6 dígitos para <strong>{usuario?.email}</strong>.</p>

        {codigoDemo && (
          <p className="dica">
            <strong>Modo demonstração:</strong> o envio de e-mail não está configurado, então o código aparece aqui:{' '}
            <code className="codigo-demo">{codigoDemo}</code>
          </p>
        )}

        <div className="codigo-caixas">
          {codigo.map((d, i) => (
            <input key={i} ref={(el) => (caixas.current[i] = el)} value={d} inputMode="numeric" maxLength={DIGITOS}
                   autoFocus={i === 0} aria-label={`Dígito ${i + 1}`} autoComplete="one-time-code"
                   onChange={(e) => digitar(i, e.target.value)} onKeyDown={(e) => apagar(i, e)} />
          ))}
        </div>
        {erro && <p className="erro">{erro}</p>}

        <button className="botao-link" disabled={espera > 0} onClick={reenviar}>
          {espera > 0 ? `Reenviar código em ${espera}s` : 'Reenviar código'}
        </button>
      </div>
    </div>
  )
}
