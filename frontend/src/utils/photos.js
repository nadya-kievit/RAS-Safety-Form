export const MAX_PHOTOS = 5
export const MAX_PHOTO_BYTES = 10 * 1024 * 1024
export const PHOTO_ACCEPT = 'image/jpeg,image/png,image/webp,image/gif'

const EXTENSIONS = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
  'image/gif': 'gif',
}

const HEIF_BRANDS = ['heic', 'heix', 'hevc', 'hevx', 'mif1', 'msf1']

function readBytes(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(new Uint8Array(reader.result))
    reader.onerror = () => reject(reader.error)
    reader.readAsArrayBuffer(blob)
  })
}

function ascii(bytes, start, end) {
  return String.fromCharCode(...bytes.slice(start, end))
}

/** Identifies an image from its first bytes: a supported MIME type, 'heif', or null. */
export function sniffImageType(bytes) {
  if (bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) return 'image/jpeg'
  if (ascii(bytes, 1, 4) === 'PNG' && bytes[0] === 0x89) return 'image/png'
  if (ascii(bytes, 0, 4) === 'RIFF' && ascii(bytes, 8, 12) === 'WEBP') return 'image/webp'
  if (ascii(bytes, 0, 6) === 'GIF87a' || ascii(bytes, 0, 6) === 'GIF89a') return 'image/gif'
  if (ascii(bytes, 4, 8) === 'ftyp' && HEIF_BRANDS.includes(ascii(bytes, 8, 12))) return 'heif'
  return null
}

function displayName(file) {
  return file.name || 'This file'
}

/**
 * Validates a picked photo by its contents, not its reported type. Phones and browsers often
 * report a blank or non-standard type (for example image/jpg) for valid photos, so the file is
 * re-typed from its bytes. Resolves to { file } or { error }.
 */
export async function preparePhoto(file) {
  let bytes
  try {
    bytes = await readBytes(file.slice(0, 12))
  } catch {
    return { error: `${displayName(file)} could not be read. Please choose it again.` }
  }
  const type = sniffImageType(bytes)

  if (type === 'heif') {
    return {
      error: `${displayName(file)} is a HEIC/HEIF photo, which is not supported. `
        + 'Choose a JPEG or PNG photo, or set your camera to its most compatible format.',
    }
  }
  if (!type) {
    return { error: `${displayName(file)} is not a supported JPEG, PNG, WebP, or GIF image.` }
  }
  if (file.size > MAX_PHOTO_BYTES) {
    return { error: `${displayName(file)} is larger than 10 MB.` }
  }

  const name = file.name || `photo.${EXTENSIONS[type]}`
  if (file.type === type && file.name) return { file }
  return { file: new File([file], name, { type, lastModified: file.lastModified }) }
}

export function photoKey(file) {
  return `${file.name}:${file.size}:${file.lastModified}`
}
