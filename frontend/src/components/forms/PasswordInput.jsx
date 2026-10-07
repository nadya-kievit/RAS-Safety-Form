import { useState } from 'react'
import { Eye, EyeOff } from 'lucide-react'

function PasswordInput({ id, ...inputProps }) {
  const [isVisible, setIsVisible] = useState(false)

  return (
    <div className="password-input-control">
      <input id={id} {...inputProps} type={isVisible ? 'text' : 'password'} />
      <button
        className="password-visibility-toggle"
        type="button"
        aria-label={isVisible ? 'Hide password' : 'Show password'}
        aria-controls={id}
        aria-pressed={isVisible}
        onClick={() => setIsVisible((current) => !current)}
      >
        {isVisible ? <EyeOff aria-hidden="true" /> : <Eye aria-hidden="true" />}
      </button>
    </div>
  )
}

export default PasswordInput
