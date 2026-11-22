import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, it, expect, vi } from 'vitest'
import RegisterPage from './RegisterPage'

const registerMock = vi.hoisted(() => vi.fn())
vi.mock('../context/AuthContext', () => ({ useAuth: () => ({ register: registerMock }) }))

describe('RegisterPage', () => {
  it('só habilita o cadastro após aceitar termos e privacidade', () => {
    render(<MemoryRouter><RegisterPage /></MemoryRouter>)
    const button = screen.getByRole('button', { name: /criar conta/i }) as HTMLButtonElement
    expect(button.disabled).toBe(true)

    const checkboxes = screen.getAllByRole('checkbox')
    fireEvent.click(checkboxes[1])
    expect(button.disabled).toBe(true)
    fireEvent.click(checkboxes[2])
    expect(button.disabled).toBe(false)
  })
})
