// Formatação de valores para o ecrã.

const moeda = new Intl.NumberFormat('pt-PT', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const numero = new Intl.NumberFormat('pt-PT', { maximumFractionDigits: 3 })

export function dinheiro(valor) {
  return `${moeda.format(Number(valor) || 0)} MT`
}

export function quantidade(valor) {
  return numero.format(Number(valor) || 0)
}

/** 135 -> "2h 15min" */
export function minutos(total) {
  const m = Math.max(0, Math.round(Number(total) || 0))
  const h = Math.floor(m / 60)
  const resto = m % 60
  if (h === 0) return `${resto}min`
  return resto === 0 ? `${h}h` : `${h}h ${String(resto).padStart(2, '0')}min`
}

/** "08:00:00" -> "08:00" */
export function hora(valor) {
  return valor ? String(valor).slice(0, 5) : ''
}

/** "2026-11-13" -> "13/11/2026" */
export function data(valor) {
  if (!valor) return ''
  const [a, m, d] = String(valor).slice(0, 10).split('-')
  return `${d}/${m}/${a}`
}

export function dataHora(valor) {
  if (!valor) return ''
  return `${data(valor)} ${hora(String(valor).slice(11))}`
}

/** Data de hoje no formato AAAA-MM-DD (hora local). */
export function hoje() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

export const ESTADOS = {
  PENDENTE: 'Pendente',
  EM_PRODUCAO: 'Em produção',
  CONCLUIDA: 'Concluída',
  CANCELADA: 'Cancelada',
}

export function numeroOuNulo(valor) {
  if (valor === '' || valor === null || valor === undefined) return null
  const n = Number(String(valor).replace(',', '.'))
  return Number.isFinite(n) ? n : null
}
