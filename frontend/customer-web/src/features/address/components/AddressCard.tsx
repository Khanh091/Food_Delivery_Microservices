import { addressLabel, addressSummary, type DeliveryAddress } from '../types/address'

interface AddressCardProps {
  address: DeliveryAddress
  busy?: boolean
  onEdit: (address: DeliveryAddress) => void
  onSetDefault: (address: DeliveryAddress) => void
  onDelete: (address: DeliveryAddress) => void
}

export function AddressCard({ address, busy, onEdit, onSetDefault, onDelete }: AddressCardProps) {
  return (
    <article className="address-card">
      <div className="address-card-heading">
        <div>
          <h2>{addressLabel(address)}</h2>
          {address.isDefault && <span className="badge">Mặc định</span>}
        </div>
        <div className="address-card-actions">
          <button className="button text" type="button" onClick={() => onEdit(address)} disabled={busy}>Sửa</button>
          {!address.isDefault && <button className="button text" type="button" onClick={() => onSetDefault(address)} disabled={busy}>Đặt mặc định</button>}
          <button className="button text danger" type="button" onClick={() => onDelete(address)} disabled={busy}>Xóa</button>
        </div>
      </div>
      <p className="address-recipient">{address.recipientName} · {address.recipientPhone}</p>
      <p>{addressSummary(address)}</p>
      {address.deliveryNote && <p className="address-note">Ghi chú: {address.deliveryNote}</p>}
    </article>
  )
}
