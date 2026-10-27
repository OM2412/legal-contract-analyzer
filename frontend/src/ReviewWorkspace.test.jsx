import { afterEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import ReviewWorkspace from './ReviewWorkspace'
import { apiFetch } from './apiClient'

vi.mock('./apiClient', () => ({ apiFetch: vi.fn() }))
afterEach(() => { cleanup(); apiFetch.mockReset() })

it('aborts an upload on unmount and ignores a late response containing private data', async () => {
  let complete
  let signal
  apiFetch.mockImplementation((url, options) => {
    signal = options.signal
    return new Promise((resolve) => { complete = resolve })
  })
  const onReview = vi.fn()
  const { container, unmount } = render(<ReviewWorkspace onReview={onReview} />)
  for (const name of ['agreement', 'sow']) {
    fireEvent.change(container.querySelector(`input[name="${name}"]`), {
      target: { files: [new File(['private'], `${name}.txt`, { type: 'text/plain' })] },
    })
  }
  fireEvent.click(screen.getByRole('button', { name: /Review payment terms/ }))
  await waitFor(() => expect(complete).toBeTypeOf('function'))
  unmount()
  expect(signal.aborted).toBe(true)
  complete({ ok: true, json: async () => ({ private: 'late result' }) })
  await new Promise((resolve) => setTimeout(resolve, 0))
  expect(onReview).not.toHaveBeenCalled()
})
