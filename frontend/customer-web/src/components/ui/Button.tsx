import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react'

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'
type ButtonSize = 'default' | 'compact'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  loading?: boolean
  icon?: ReactNode
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button({ children, className, disabled, icon, loading = false, size = 'default', type = 'button', variant = 'primary', ...props }, ref) {
  const classes = ['ui-button', `ui-button-${variant}`, `ui-button-${size}`, className].filter(Boolean).join(' ')

  return (
    <button ref={ref} type={type} className={classes} disabled={disabled || loading} {...props}>
      {loading ? <span className="ui-button-spinner" aria-hidden="true" /> : icon ? <span className="ui-button-icon" aria-hidden="true">{icon}</span> : null}
      {children ? <span>{children}</span> : null}
    </button>
  )
})
