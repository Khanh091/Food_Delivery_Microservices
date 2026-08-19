interface RestaurantToggleProps {
  checked: boolean
  onChange: (checked: boolean) => void
  label?: string
  disabled?: boolean
  busy?: boolean
}

export function RestaurantToggle({ checked, onChange, label, disabled = false, busy = false }: RestaurantToggleProps) {
  return (
    <button
      type="button"
      className={`owner-toggle${checked ? ' is-on' : ''}`}
      role="switch"
      aria-checked={checked}
      aria-label={label}
      disabled={disabled || busy}
      onClick={() => onChange(!checked)}
    >
      <span className="owner-toggle-track" aria-hidden="true"><span className="owner-toggle-thumb" /></span>
      {label ? <span className="owner-toggle-label">{busy ? 'Đang cập nhật…' : label}</span> : null}
    </button>
  )
}