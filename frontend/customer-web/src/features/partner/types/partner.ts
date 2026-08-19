export type PartnerApplicationStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'NEEDS_MORE_INFORMATION' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type BusinessType = 'HOUSEHOLD_BUSINESS' | 'COMPANY' | 'INDIVIDUAL' | 'FRANCHISE' | 'OTHER'
export type ApplicationDocumentType = 'BUSINESS_LICENSE' | 'FOOD_SAFETY_CERTIFICATE' | 'OWNER_ID_CARD' | 'TAX_DOCUMENT' | 'BANK_DOCUMENT' | 'OTHER'
export type RestaurantBranchStatus = 'PENDING' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'CLOSED'

export interface RestaurantApplicationSummary { id: string; businessName: string; status: PartnerApplicationStatus; city: string; submittedAt: string | null; createdAt: string }
export interface RestaurantApplication extends RestaurantApplicationSummary {
  applicantUserId: string; businessType: BusinessType | null; taxCode: string | null; representativeName: string; representativePhone: string; representativeEmail: string | null; description: string | null; district: string | null; businessAddress: string; expectedBranchCount: number; estimatedDailyOrders: number | null; mainCuisine: string | null; reviewedAt: string | null; rejectionReason: string | null; updatedAt: string; version: number
}
export interface ApplicationDocument { id: string; applicationId: string; documentType: ApplicationDocumentType; documentNumber: string | null; fileName: string; fileUrl: string; mimeType: string; fileSize: number; verificationStatus: string; issuedAt: string | null; expiresAt: string | null; createdAt: string; updatedAt: string }
export interface ApplicationInput { businessName: string; businessType: BusinessType | ''; taxCode: string; representativeName: string; representativePhone: string; representativeEmail: string; description: string; city: string; district: string; businessAddress: string; expectedBranchCount: number; estimatedDailyOrders: number | null; mainCuisine: string }
export interface RestaurantSummary { id: string; name: string; restaurantCode: string; status: string; verificationStatus: string }
export interface Restaurant extends RestaurantSummary { ownerUserId: string; partnerApplicationId: string; legalName: string | null; description: string | null; logoUrl: string | null; coverImageUrl: string | null; phoneNumber: string | null; email: string | null; taxCode: string | null; averageRating: number; totalReviews: number; createdAt: string; updatedAt: string; version: number }
export interface RestaurantBranch {
  id: string
  restaurantId?: string
  branchCode: string
  name: string
  phoneNumber: string | null
  email: string | null
  addressLine: string
  ward: string | null
  district: string | null
  city: string
  latitude?: number | null
  longitude?: number | null
  status: RestaurantBranchStatus | string
  acceptingOrders: boolean
  minimumOrderAmount: number | null
  defaultPreparationMinutes: number | null
  createdAt?: string
  updatedAt?: string
  version?: number
}
export interface RestaurantBranchCreateInput {
  branchCode: string
  name: string
  phoneNumber?: string | null
  email?: string | null
  addressLine: string
  ward?: string | null
  district?: string | null
  city?: string | null
  latitude: number
  longitude: number
  minimumOrderAmount: number
  defaultPreparationMinutes: number
}
export interface RestaurantBranchUpdateInput {
  name?: string
  phoneNumber?: string | null
  email?: string | null
  addressLine?: string
  ward?: string | null
  district?: string | null
  city?: string | null
  latitude?: number
  longitude?: number
  minimumOrderAmount?: number
  defaultPreparationMinutes?: number
  status?: RestaurantBranchStatus
}
export interface BranchBusinessHour {
  id: string
  branchId: string
  dayOfWeek: number
  openTime: string | null
  closeTime: string | null
  closed: boolean
  version: number
}
export interface BranchBusinessHoursInput {
  hours: { dayOfWeek: number; openTime?: string | null; closeTime?: string | null; isClosed: boolean }[]
}
export interface BranchSpecialHour {
  id: string
  branchId: string
  specialDate: string
  openTime: string | null
  closeTime: string | null
  closed: boolean
  reason: string | null
  version: number
}
export type RestaurantMemberRole = 'OWNER' | 'MANAGER' | 'CATALOG_MANAGER' | 'ORDER_OPERATOR' | 'ACCOUNTANT' | 'STAFF'
export type RestaurantMemberStatus = 'INVITED' | 'ACTIVE' | 'SUSPENDED' | 'REMOVED' | 'REJECTED'
export interface RestaurantMember {
  id: string
  restaurantId: string
  userId: string
  fullName: string | null
  email: string | null
  phoneNumber: string | null
  avatarUrl: string | null
  branchId: string | null
  branchName: string
  role: RestaurantMemberRole
  status: RestaurantMemberStatus
  invitedByUserId: string | null
  joinedAt: string | null
  createdAt: string
  version: number
}
export interface RestaurantMemberCreateInput { email: string; branchId: string | null; role: Exclude<RestaurantMemberRole, 'OWNER'> }
export interface RestaurantMemberUpdateInput { role: Exclude<RestaurantMemberRole, 'OWNER'>; status: Exclude<RestaurantMemberStatus, 'REMOVED'>; branchId: string | null; updateBranchScope: true }
export interface RestaurantBankAccount { id: string; bankCode: string; bankName: string; maskedAccountNumber: string; accountHolderName: string; defaultAccount: boolean; verificationStatus: string }
export interface RestaurantBankAccountCreateInput { bankCode: string; bankName?: string | null; accountNumber: string; accountHolderName: string }
export interface RestaurantBankAccountUpdateInput { bankName?: string | null; accountHolderName?: string | null }
