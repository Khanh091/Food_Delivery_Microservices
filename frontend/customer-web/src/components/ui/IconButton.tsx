import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react'

type IconButtonVariant = 'ghost' | 'danger'

interface IconButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  icon: ReactNode
  label: string
  variant?: IconButtonVariant
}

export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(function IconButton({ className, icon, label, type = 'button', variant = 'ghost', ...props }, ref) {
  const classes = ['ui-icon-button', `ui-icon-button-${variant}`, className].filter(Boolean).join(' ')

  return <button ref={ref} type={type} className={classes} aria-label={label} title={label} {...props}>{icon}</button>
})
