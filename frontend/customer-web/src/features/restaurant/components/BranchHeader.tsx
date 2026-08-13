import type { PublicRestaurantBranch } from '../types/restaurant'

const address = (branch: PublicRestaurantBranch) =>
  [branch.addressLine, branch.ward, branch.district, branch.city]
    .filter((part): part is string => Boolean(part?.trim()))
    .join(', ')

export function BranchHeader({ branch }: { branch: PublicRestaurantBranch }) {
  const restaurantName = branch.restaurantName.trim() || 'Cửa hàng'
  const branchName = branch.branchName.trim()
  const showBranchName = branchName.localeCompare(restaurantName, undefined, { sensitivity: 'accent' }) !== 0

  return (
    <section className="branch-hero">
      <div className="branch-hero-brand">
        {branch.restaurantLogoUrl ? (
          <img className="branch-hero-logo" src={branch.restaurantLogoUrl} alt={`Logo ${restaurantName}`} />
        ) : (
          <div className="branch-hero-placeholder" aria-hidden="true">{restaurantName.slice(0, 1).toUpperCase()}</div>
        )}
      </div>
      <div className="branch-hero-content">
        <p className="eyebrow">Cửa hàng</p>
        <h1>{restaurantName}</h1>
        {showBranchName && <p className="branch-hero-name">{branchName}</p>}
        {address(branch) && <p className="branch-hero-address">{address(branch)}</p>}
        <div className="branch-hero-meta">
          <span className={branch.acceptingOrders ? 'branch-status accepting' : 'branch-status paused'}>
            {branch.acceptingOrders ? 'Đang nhận đơn' : 'Hiện không nhận đơn'}
          </span>
          {branch.phoneNumber && <a href={`tel:${branch.phoneNumber}`} className="branch-phone">{branch.phoneNumber}</a>}
        </div>
        {branch.restaurantDescription?.trim() && <p className="branch-hero-description">{branch.restaurantDescription}</p>}
      </div>
    </section>
  )
}
