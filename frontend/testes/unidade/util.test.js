import { describe, expect, it } from 'vitest'
import { data, dinheiro, hora, minutos, numeroOuNulo, quantidade } from '../../src/util.js'

// Testes de UNIDADE: funções puras de formatação
describe('formatação', () => {
  it('formata valores em meticais', () => {
    expect(dinheiro(3120)).toMatch(/3.?120,00 MT/)
    expect(dinheiro(null)).toBe('0,00 MT')
  })

  it('converte minutos em horas e minutos', () => {
    expect(minutos(45)).toBe('45min')
    expect(minutos(120)).toBe('2h')
    expect(minutos(135)).toBe('2h 15min')
    expect(minutos(-5)).toBe('0min')
  })

  it('mostra só horas e minutos', () => {
    expect(hora('08:30:00')).toBe('08:30')
    expect(hora(null)).toBe('')
  })

  it('converte datas ISO para dd/mm/aaaa', () => {
    expect(data('2026-11-13')).toBe('13/11/2026')
    expect(data('2026-11-13T09:06:52')).toBe('13/11/2026')
  })

  it('lê números com vírgula ou ponto', () => {
    expect(numeroOuNulo('2,5')).toBe(2.5)
    expect(numeroOuNulo('10')).toBe(10)
    expect(numeroOuNulo('')).toBeNull()
    expect(numeroOuNulo('abc')).toBeNull()
  })

  it('formata quantidades com até 3 casas decimais', () => {
    expect(quantidade(0.2)).toBe('0,2')
  })
})
