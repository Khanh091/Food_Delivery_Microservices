export interface ApiResponse<T> {
  success?: boolean;
  code?: string | number;
  message?: string | null;
  data?: T;
  timestamp?: string;
  path?: string;
}

export interface ApiErrorPayload {
  message?: string | null;
  error?: string;
  code?: string | number;
  errors?: Record<string, string>;
}
