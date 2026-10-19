// Todas as chamadas ao back-end passam por aqui.
// O login fica num cookie de sessão; nos POST/PUT/DELETE mandamos o token CSRF que o Spring colocou no cookie XSRF-TOKEN.

export class ErroApi extends Error {
  constructor(mensagem, status, dados) {
    super(mensagem)
    this.status = status
    this.dados = dados
  }
}

function lerCookie(nome) {
  const cookie = document.cookie.split('; ').find((c) => c.startsWith(nome + '='))
  return cookie ? decodeURIComponent(cookie.split('=')[1]) : null
}

export async function api(caminho, { metodo = 'GET', corpo, formulario } = {}) {
  const headers = { Accept: 'application/json' }
  let body

  if (metodo !== 'GET') {
    const token = lerCookie('XSRF-TOKEN')
    if (token) headers['X-XSRF-TOKEN'] = token
  }
  if (formulario) {
    body = formulario // FormData (envio de foto): o navegador define o Content-Type
  } else if (corpo !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(corpo)
  }

  const resposta = await fetch('/api' + caminho, { method: metodo, headers, body, credentials: 'same-origin' })
  const texto = await resposta.text()
  const dados = texto ? JSON.parse(texto) : null

  if (!resposta.ok) {
    const mensagem = dados?.erro || (resposta.status === 401 ? 'Faça login para continuar' : 'Algo deu errado. Tente de novo.')
    throw new ErroApi(mensagem, resposta.status, dados)
  }
  return dados
}

export const get = (caminho) => api(caminho)
export const post = (caminho, corpo) => api(caminho, { metodo: 'POST', corpo })
export const put = (caminho, corpo) => api(caminho, { metodo: 'PUT', corpo })
export const del = (caminho) => api(caminho, { metodo: 'DELETE' })
