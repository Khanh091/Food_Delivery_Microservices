import type { ReactNode } from 'react'

interface SectionHeaderProps {
  title: string
  description?: string
  actions?: ReactNode
  className?: string
}

export function SectionHeader({ actions, className, description, title }: SectionHeaderProps) {
  return (
    <header className={['ui-section-header', className].filter(Boolean).join(' ')}>
      <div className="ui-section-header-copy">
        <h2>{title}</h2>
        {description ? <p>{description}</p> : null}
      </div>
      {actions ? <div className="ui-section-header-actions">{actions}</div> : null}
    </header>
  )
}
