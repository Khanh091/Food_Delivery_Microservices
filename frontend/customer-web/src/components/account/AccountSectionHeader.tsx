import type { ReactNode } from 'react'

interface AccountSectionHeaderProps {
  eyebrow: string
  title: string
  description: string
  action?: ReactNode
  titleId?: string
}

export function AccountSectionHeader({ eyebrow, title, description, action, titleId }: AccountSectionHeaderProps) {
  return (
    <header className={`account-section-header${action ? ' has-action' : ''}`}>
      <div className="account-section-header-copy">
        <p className="eyebrow">{eyebrow}</p>
        <h1 id={titleId}>{title}</h1>
        <p>{description}</p>
      </div>
      {action ? <div className="account-section-header-action">{action}</div> : null}
    </header>
  )
}
