export type PartnerApplicationStatus = 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'NEEDS_MORE_INFORMATION' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type BusinessType = 'HOUSEHOLD_BUSINESS' | 'COMPANY' | 'INDIVIDUAL' | 'FRANCHISE' | 'OTHER'
export type ApplicationDocumentType = 'BUSINESS_LICENSE' | 'FOOD_SAFETY_CERTIFICATE' | 'OWNER_ID_CARD' | 'TAX_DOCUMENT' | 'BANK_DOCUMENT' | 'OTHER'

export interface RestaurantApplicationSummary { id: string; businessName: string; status: PartnerApplicationStatus; city: string; submittedAt: string | null; createdAt: string }
export interface RestaurantApplication extends RestaurantApplicationSummary {
  applicantUserId: string; businessType: BusinessType | null; taxCode: string | null; representativeName: string; representativePhone: string; representativeEmail: string | null; description: string | null; district: string | null; businessAddress: string; expectedBranchCount: number; estimatedDailyOrders: number | null; mainCuisine: string | null; reviewedAt: string | null; rejectionReason: string | null; updatedAt: string; version: number
}
export interface ApplicationDocument { id: string; applicationId: string; documentType: ApplicationDocumentType; documentNumber: string | null; fileName: string; fileUrl: string; mimeType: string; fileSize: number; verificationStatus: string; issuedAt: string | null; expiresAt: string | null; createdAt: string; updatedAt: string }
export interface ApplicationInput { businessName: string; businessType: BusinessType | ''; taxCode: string; representativeName: string; representativePhone: string; representativeEmail: string; description: string; city: string; district: string; businessAddress: string; expectedBranchCount: number; estimatedDailyOrders: number | null; mainCuisine: string }
export interface RestaurantSummary { id: string; name: string; restaurantCode: string; status: string; verificationStatus: string }
export interface Restaurant extends RestaurantSummary { ownerUserId: string; partnerApplicationId: string; legalName: string | null; description: string | null; logoUrl: string | null; coverImageUrl: string | null; phoneNumber: string | null; email: string | null; taxCode: string | null; averageRating: number; totalReviews: number; createdAt: string; updatedAt: string; version: number }
export interface RestaurantBranch { id: string; branchCode: string; name: string; phoneNumber: string | null; email: string | null; addressLine: string; ward: string | null; district: string | null; city: string; status: string; acceptingOrders: boolean; minimumOrderAmount: number | null; defaultPreparationMinutes: number | null }
export interface RestaurantMember { id: string; userId: string; branchId: string | null; role: string; status: string; joinedAt: string | null }
export interface RestaurantBankAccount { id: string; bankCode: string; bankName: string; maskedAccountNumber: string; accountHolderName: string; defaultAccount: boolean; verificationStatus: string }
