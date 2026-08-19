import { PencilIcon } from '../../../components/icons/PencilIcon'
import { PlusIcon } from '../../../components/icons/PlusIcon'
import { TrashIcon } from '../../../components/icons/TrashIcon'
import { Button } from '../../../components/ui/Button'
import { IconButton } from '../../../components/ui/IconButton'
import { SectionHeader } from '../../../components/ui/SectionHeader'
import { RestaurantEmptyState } from '../../partner/components/RestaurantEmptyState'
import { RestaurantStatusBadge } from '../../partner/components/RestaurantStatusBadge'
import type { CatalogCategory, CatalogMenu } from '../types/catalog'

interface CatalogSidebarProps {
  branchName: string | null
  menus: CatalogMenu[] | null
  selectedMenu: CatalogMenu | null
  categories: CatalogCategory[] | null
  categoryError?: string | null
  selectedCategoryId: string | null
  onCreateMenu: () => void
  onSelectMenu: (menuId: string) => void
  onCreateCategory: () => void
  onEditCategory: (category: CatalogCategory) => void
  onDeleteCategory: (category: CatalogCategory) => void
  onSelectCategory: (categoryId: string) => void
}

export function CatalogSidebar({ branchName, categories, categoryError, menus, onCreateCategory, onCreateMenu, onDeleteCategory, onEditCategory, onSelectCategory, onSelectMenu, selectedCategoryId, selectedMenu }: CatalogSidebarProps) {
  return (
    <aside className="catalog-navigation">
      <SectionHeader
        className="catalog-sidebar-heading"
        title="Thực đơn"
        description={branchName ?? 'Chi nhánh đang chọn'}
        actions={<Button variant="ghost" size="compact" icon={<PlusIcon />} onClick={onCreateMenu}>Thêm</Button>}
      />
      {menus?.length ? (
        <div className="catalog-menu-list">
          {menus.map((value) => (
            <button type="button" key={value.id} className={`catalog-menu-button${value.id === selectedMenu?.id ? ' active' : ''}`} onClick={() => onSelectMenu(value.id)}>
              <span>{value.name}</span>
              <RestaurantStatusBadge status={value.status} label={value.status === 'ACTIVE' ? 'Đang bật' : 'Tạm ngưng'} />
            </button>
          ))}
        </div>
      ) : (
        <RestaurantEmptyState title="Chưa có thực đơn" description="Tạo thực đơn đầu tiên cho chi nhánh này." action={<Button variant="secondary" size="compact" icon={<PlusIcon />} onClick={onCreateMenu}>Thêm thực đơn</Button>} />
      )}

      {selectedMenu ? (
        <>
          <div className="catalog-nav-divider" />
          <SectionHeader
            className="catalog-sidebar-heading"
            title="Danh mục"
            actions={<Button variant="ghost" size="compact" icon={<PlusIcon />} onClick={onCreateCategory}>Thêm</Button>}
          />
          {categoryError ? <p className="catalog-sidebar-loading">{categoryError}</p> : categories === null ? <p className="catalog-sidebar-loading">Đang tải danh mục…</p> : categories.length ? (
            <div className="catalog-category-list">
              {categories.map((value) => (
                <div className={`catalog-category-row${value.id === selectedCategoryId ? ' active' : ''}`} key={value.id}>
                  <button type="button" className={`catalog-category-button${value.id === selectedCategoryId ? ' active' : ''}`} onClick={() => onSelectCategory(value.id)}><span>{value.name}</span></button>
                  <div className="catalog-category-row-actions">
                    <IconButton icon={<PencilIcon />} label={`Chỉnh sửa ${value.name}`} onClick={() => onEditCategory(value)} />
                    <IconButton icon={<TrashIcon />} label={`Xóa ${value.name}`} variant="danger" onClick={() => onDeleteCategory(value)} />
                  </div>
                </div>
              ))}
            </div>
          ) : <RestaurantEmptyState title="Chưa có danh mục" description="Tạo danh mục để sắp xếp món ăn." action={<Button variant="secondary" size="compact" icon={<PlusIcon />} onClick={onCreateCategory}>Thêm danh mục</Button>} />}
        </>
      ) : null}
    </aside>
  )
}
