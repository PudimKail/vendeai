const formatoMoeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

/** 1899 -> "R$ 1.899,00" */
export const moeda = (valor) => formatoMoeda.format(Number(valor ?? 0))

/** "2026-10-19T08:30:00" -> "19/10/2026 08:30" */
export const dataHora = (iso) =>
  iso ? new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : ''

/** Tempo relativo para as notificações: "agora", "há 5 min", "há 2 h", "ontem"... */
export function haQuanto(iso) {
  const minutos = Math.floor((Date.now() - new Date(iso).getTime()) / 60000)
  if (minutos < 1) return 'agora'
  if (minutos < 60) return `há ${minutos} min`
  const horas = Math.floor(minutos / 60)
  if (horas < 24) return `há ${horas} h`
  const dias = Math.floor(horas / 24)
  return dias === 1 ? 'ontem' : `há ${dias} dias`
}
