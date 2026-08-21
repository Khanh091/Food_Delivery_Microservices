import type { ApiErrorPayload } from "../../types/api";

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | number | undefined;
  readonly details: Record<string, string> | undefined;

  constructor(status: number, payload?: ApiErrorPayload, fallback?: string) {
    super(
      payload?.message ??
        payload?.error ??
        fallback ??
        "Không thể kết nối máy chủ.",
    );
    this.name = "ApiError";
    this.status = status;
    this.code = payload?.code;
    this.details = payload?.errors;
  }
}

export const isApiError = (error: unknown): error is ApiError =>
  error instanceof ApiError;
