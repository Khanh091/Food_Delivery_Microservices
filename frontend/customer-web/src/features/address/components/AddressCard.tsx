import { addressLabel, addressSummary, type DeliveryAddress } from '../types/address'

interface AddressCardProps {
  address: DeliveryAddress
  busy?: boolean
  onEdit: (address: DeliveryAddress) => void
  onSetDefault: (address: DeliveryAddress) => void
  onDelete: (address: DeliveryAddress) => void
}

export function AddressCard({ address, busy, onEdit, onSetDefault, onDelete }: AddressCardProps) {
  const detail = [address.buildingName, address.floor && `Tầng ${address.floor}`, address.entrance && `Lối vào: ${address.entrance}`]
    .filter(Boolean)
    .join(' · ')

  return (
    <article className={`address-card${address.isDefault ? ' is-default' : ''}`}>
      <div className="address-card-content">
        <div className="address-card-heading">
          <div className="address-card-title">
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
        {detail && <p className="address-location">{detail}</p>}
        <p className="address-location">{addressSummary(address)}</p>
        {address.deliveryNote && <p className="address-note">Ghi chú giao hàng: {address.deliveryNote}</p>}
      </div>
    </article>
  )
}
