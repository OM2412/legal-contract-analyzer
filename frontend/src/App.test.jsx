import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import App from './App'
import { apiFetch } from './apiClient'

vi.mock('./apiClient', () => ({ apiFetch: vi.fn() }))
vi.mock('./HomePage', () => ({ default: () => <div>Home</div> }))
vi.mock('./AuthPage', () => ({ default: ({ onAuthenticated }) => <button onClick={onAuthenticated}>Test sign in</button> }))
vi.mock('./ReviewWorkspace', () => ({ default: ({ onReview }) => <button onClick={() => onReview({ reviewStatus: 'TEST' }, { agreement: 'private' })}>Make private review</button> }))
vi.mock('./ReviewResult', () => ({ default: () => <div>Confidential result</div> }))

beforeEach(() => {
  window.location.hash = '#workspace'
  vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
  apiFetch.mockReset()
  apiFetch.mockResolvedValue({ ok: true, status: 200 })
})
afterEach(cleanup)

async function privateReview() {
  render(<App />)
  fireEvent.click(await screen.findByText('Make private review'))
  await screen.findByText('Confidential result')
}

it('clears private content, confirms server logout and prevents reuse after another sign-in', async () => {
  await privateReview()
  apiFetch.mockResolvedValue({ ok: true, status: 204 })
  fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))
  await screen.findByText('Test sign in')
  expect(apiFetch).toHaveBeenCalledWith('/api/auth/logout', { method: 'POST' })
  expect(screen.queryByText('Confidential result')).toBeNull()
  fireEvent.click(screen.getByText('Test sign in'))
  window.location.hash = '#review'
  fireEvent(window, new Event('hashchange'))
  await screen.findByText('Your next review starts here.')
  expect(screen.queryByText('Confidential result')).toBeNull()
})

it('clears content before logout completes and blocks private screens when network confirmation fails', async () => {
  await privateReview()
  let rejectLogout
  apiFetch.mockImplementation(() => new Promise((resolve, reject) => { rejectLogout = reject }))
  fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))
  expect(screen.queryByText('Confidential result')).toBeNull()
  await waitFor(() => expect(rejectLogout).toBeTypeOf('function'))
  rejectLogout(new TypeError('Offline'))
  await screen.findByRole('alert')
  expect(screen.queryByText('Test sign in')).toBeNull()
  apiFetch.mockResolvedValue({ ok: true, status: 204 })
  fireEvent.click(screen.getByText('Retry sign out'))
  await screen.findByText('Test sign in')
})

it('treats an already-expired server session as signed out', async () => {
  await privateReview()
  apiFetch.mockResolvedValue({ ok: false, status: 401 })
  fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))
  await screen.findByText('Test sign in')
  expect(screen.queryByText('Confidential result')).toBeNull()
})
