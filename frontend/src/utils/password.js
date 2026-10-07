export const PASSWORD_MIN_LENGTH = 8
export const PASSWORD_MAX_LENGTH = 20

export function validateNewPassword(password, confirmation) {
  const length = [...password].length
  if (length < PASSWORD_MIN_LENGTH) {
    return `Password must be at least ${PASSWORD_MIN_LENGTH} characters.`
  }
  if (length > PASSWORD_MAX_LENGTH) {
    return `Password must be no more than ${PASSWORD_MAX_LENGTH} characters.`
  }
  if (!/[0-9]/u.test(password)) {
    return 'Password must contain at least one number.'
  }
  const hasSymbol = [...password].some((character) =>
    !/[\p{L}\p{N}\s]/u.test(character))
  if (!hasSymbol) {
    return 'Password must contain at least one non-alphanumeric character.'
  }
  if (password !== confirmation) {
    return 'Passwords do not match.'
  }
  return ''
}
