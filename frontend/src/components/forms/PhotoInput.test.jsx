import { useState } from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import PhotoInput from './PhotoInput.jsx'

const JPEG = [0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46, 0x00, 0x01]
const HEIC = [0, 0, 0, 24, 102, 116, 121, 112, 104, 101, 105, 99]

function jpeg(name, lastModified = 1) {
  return new File([new Uint8Array(JPEG)], name, { type: 'image/jpeg', lastModified })
}

function Harness() {
  const [photos, setPhotos] = useState([])
  const [error, setError] = useState('')
  return (
    <>
      <PhotoInput
        photos={photos}
        error={error}
        onChange={(next, message) => {
          setPhotos(next)
          setError(message)
        }}
      />
      <output data-testid="count">{photos.length}</output>
    </>
  )
}

describe('PhotoInput', () => {
  let revoke

  beforeEach(() => {
    let counter = 0
    URL.createObjectURL = vi.fn(() => `blob:preview-${counter++}`)
    revoke = vi.fn()
    URL.revokeObjectURL = revoke
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('previews several photos selected at once', async () => {
    const user = userEvent.setup()
    render(<Harness />)

    await user.upload(screen.getByLabelText('Photos'), [jpeg('a.jpg', 1), jpeg('b.jpg', 2)])

    expect(await screen.findByAltText('Preview of a.jpg')).toBeInTheDocument()
    expect(screen.getByAltText('Preview of b.jpg')).toBeInTheDocument()
    expect(screen.getByTestId('count')).toHaveTextContent('2')
  })

  it('adds to earlier picks instead of replacing them', async () => {
    const user = userEvent.setup()
    render(<Harness />)

    await user.upload(screen.getByLabelText('Photos'), jpeg('a.jpg', 1))
    await screen.findByAltText('Preview of a.jpg')
    await user.upload(screen.getByLabelText('Photos'), jpeg('b.jpg', 2))

    expect(await screen.findByAltText('Preview of b.jpg')).toBeInTheDocument()
    expect(screen.getByAltText('Preview of a.jpg')).toBeInTheDocument()
  })

  it('removes only the photo whose X button is pressed', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    await user.upload(screen.getByLabelText('Photos'), [jpeg('a.jpg', 1), jpeg('b.jpg', 2)])
    await screen.findByAltText('Preview of b.jpg')

    await user.click(screen.getByRole('button', { name: 'Remove a.jpg' }))

    expect(screen.queryByAltText('Preview of a.jpg')).not.toBeInTheDocument()
    expect(screen.getByAltText('Preview of b.jpg')).toBeInTheDocument()
    expect(revoke).toHaveBeenCalledTimes(1)
  })

  it('does not add the same photo twice', async () => {
    const user = userEvent.setup()
    render(<Harness />)

    await user.upload(screen.getByLabelText('Photos'), jpeg('a.jpg', 1))
    await screen.findByAltText('Preview of a.jpg')
    await user.upload(screen.getByLabelText('Photos'), jpeg('a.jpg', 1))

    await waitFor(() => expect(screen.getByTestId('count')).toHaveTextContent('1'))
  })

  it('refuses more than five photos and keeps the existing ones', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    await user.upload(
      screen.getByLabelText('Photos'),
      [1, 2, 3, 4].map((n) => jpeg(`p${n}.jpg`, n)),
    )
    await screen.findByAltText('Preview of p4.jpg')

    await user.upload(screen.getByLabelText('Photos'), [jpeg('p5.jpg', 5), jpeg('p6.jpg', 6)])

    expect(await screen.findByRole('alert')).toHaveTextContent('maximum of 5')
    expect(screen.getByTestId('count')).toHaveTextContent('4')
  })

  it('explains HEIC photos are unsupported without adding them', async () => {
    const user = userEvent.setup({ applyAccept: false })
    render(<Harness />)
    const heic = new File([new Uint8Array(HEIC)], 'IMG_1.HEIC', { type: 'image/heic' })

    await user.upload(screen.getByLabelText('Photos'), heic)

    expect(await screen.findByRole('alert')).toHaveTextContent('HEIC')
    expect(screen.getByTestId('count')).toHaveTextContent('0')
  })

  it('shows a fallback when the browser cannot decode a preview', async () => {
    const user = userEvent.setup()
    render(<Harness />)
    await user.upload(screen.getByLabelText('Photos'), jpeg('a.jpg'))

    const image = await screen.findByAltText('Preview of a.jpg')
    image.dispatchEvent(new Event('error'))

    expect(await screen.findByText('Preview unavailable')).toBeInTheDocument()
  })
})
