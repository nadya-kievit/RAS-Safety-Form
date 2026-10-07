import { describe, expect, it } from 'vitest'
import { MAX_PHOTO_BYTES, preparePhoto, sniffImageType } from './photos.js'

const ascii = (text) => [...text].map((char) => char.charCodeAt(0))

const JPEG = [0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46, 0x00, 0x01]
const PNG = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d]
const GIF = ascii('GIF89a')
const WEBP = [...ascii('RIFF'), 0, 0, 0, 0, ...ascii('WEBP')]
const HEIC = [0, 0, 0, 24, ...ascii('ftypheic')]

function file(bytes, name, type, lastModified = 1) {
  return new File([new Uint8Array(bytes)], name, { type, lastModified })
}

describe('sniffImageType', () => {
  it('recognises the formats phones and browsers produce', () => {
    expect(sniffImageType(new Uint8Array(JPEG))).toBe('image/jpeg')
    expect(sniffImageType(new Uint8Array(PNG))).toBe('image/png')
    expect(sniffImageType(new Uint8Array(GIF))).toBe('image/gif')
    expect(sniffImageType(new Uint8Array(WEBP))).toBe('image/webp')
  })

  it('flags HEIC/HEIF separately and rejects everything else', () => {
    expect(sniffImageType(new Uint8Array(HEIC))).toBe('heif')
    expect(sniffImageType(new TextEncoder().encode('%PDF-1.7 hello'))).toBeNull()
    expect(sniffImageType(new Uint8Array(3))).toBeNull()
  })
})

describe('preparePhoto', () => {
  it('accepts a correctly typed photo untouched', async () => {
    const photo = file(JPEG, 'IMG_0001.jpg', 'image/jpeg')

    const result = await preparePhoto(photo)

    expect(result.file).toBe(photo)
  })

  it('re-types photos whose reported type is blank or non-standard', async () => {
    const blank = await preparePhoto(file(JPEG, 'IMG_0002.JPG', ''))
    const legacy = await preparePhoto(file(JPEG, 'IMG_0003.jpg', 'image/jpg'))

    expect(blank.file.type).toBe('image/jpeg')
    expect(blank.file.name).toBe('IMG_0002.JPG')
    expect(legacy.file.type).toBe('image/jpeg')
  })

  it('gives nameless photos a usable filename', async () => {
    const result = await preparePhoto(file(PNG, '', ''))

    expect(result.file.name).toBe('photo.png')
    expect(result.file.type).toBe('image/png')
  })

  it('explains that HEIC photos are not supported, even when named .jpg', async () => {
    const result = await preparePhoto(file(HEIC, 'IMG_0004.jpg', 'image/jpeg'))

    expect(result.file).toBeUndefined()
    expect(result.error).toMatch(/HEIC/)
  })

  it('rejects files that are not images', async () => {
    const result = await preparePhoto(
      file(ascii('%PDF-1.7 not an image'), 'a.jpg', 'image/jpeg'),
    )

    expect(result.error).toMatch(/not a supported/)
  })

  it('rejects photos over 10 MB', async () => {
    const big = file(JPEG, 'big.jpg', 'image/jpeg')
    Object.defineProperty(big, 'size', { value: MAX_PHOTO_BYTES + 1 })

    const result = await preparePhoto(big)

    expect(result.error).toMatch(/10 MB/)
  })
})
