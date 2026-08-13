import type { DeliveryAddress } from '../types/address'
import { AddressCard } from './AddressCard'

interface AddressListProps {
  addresses: DeliveryAddress[]
  busy?: boolean
  onEdit: (address: DeliveryAddress) => void
  onSetDefault: (address: DeliveryAddress) => void
  onDelete: (address: DeliveryAddress) => void
}

export function AddressList({ addresses, busy, onEdit, onSetDefault, onDelete }: AddressListProps) {
  return (
    <div className="address-list">
      {addresses.map((address) => <AddressCard key={address.id} address={address} busy={busy} onEdit={onEdit} onSetDefault={onSetDefault} onDelete={onDelete} />)}
    </div>
  )
}
