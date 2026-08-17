import { create } from 'zustand'

export type ToastKind = 'success' | 'error' | 'info'
export interface Toast { id: number; kind: ToastKind; message: string }

interface ToastState {
  toasts: Toast[]
  push: (kind: ToastKind, message: string) => void
  dismiss: (id: number) => void
}

let nextToastId = 0
export const useToastStore = create<ToastState>((set) => ({
  toasts: [],
  push: (kind, message) => set((state) => ({ toasts: [...state.toasts, { id: ++nextToastId, kind, message }].slice(-4) })),
  dismiss: (id) => set((state) => ({ toasts: state.toasts.filter((toast) => toast.id !== id) })),
}))
