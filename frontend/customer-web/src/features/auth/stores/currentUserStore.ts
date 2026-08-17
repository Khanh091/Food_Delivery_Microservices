import { create } from 'zustand'
import { getCurrentUser, type CurrentUserProfile } from '../api/currentUserApi'

interface CurrentUserState {
  profile: CurrentUserProfile | null
  loading: boolean
  error: string | null
  loadProfile: () => Promise<CurrentUserProfile>
  setProfile: (profile: CurrentUserProfile) => void
  clearProfile: () => void
}

let pendingProfileRequest: Promise<CurrentUserProfile> | null = null
let profileRequestVersion = 0

export const useCurrentUserStore = create<CurrentUserState>((set) => ({
  profile: null,
  loading: false,
  error: null,

  loadProfile: () => {
    if (!pendingProfileRequest) {
      const requestVersion = profileRequestVersion
      set({ loading: true, error: null })
      const request = getCurrentUser()
        .then((profile) => {
          if (requestVersion === profileRequestVersion) {
            set({ profile, error: null })
          }
          return profile
        })
        .catch((error: unknown) => {
          if (requestVersion === profileRequestVersion) {
            set({ error: 'Chưa thể tải thông tin tài khoản. Vui lòng thử lại.' })
          }
          throw error
        })
        .finally(() => {
          if (pendingProfileRequest === request) {
            pendingProfileRequest = null
          }
          if (requestVersion === profileRequestVersion) {
            set({ loading: false })
          }
        })
      pendingProfileRequest = request
    }

    return pendingProfileRequest
  },

  setProfile: (profile) => set({ profile, error: null }),
  clearProfile: () => {
    profileRequestVersion += 1
    pendingProfileRequest = null
    set({ profile: null, loading: false, error: null })
  },
}))
