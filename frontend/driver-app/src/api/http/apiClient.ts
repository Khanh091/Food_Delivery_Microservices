import { ApiError } from "../errors/ApiError";
import {
  getAccessToken,
  notifyUnauthorized,
} from "../../auth/store/authSession";
import { apiConfig } from "./config";
import type { ApiResponse } from "../../types/api";

export interface ApiRequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

const isEnvelope = <T>(value: unknown): value is ApiResponse<T> =>
  typeof value === "object" &&
  value !== null &&
  ("data" in value ||
    "success" in value ||
    "code" in value ||
    "timestamp" in value);

const readJson = async (response: Response): Promise<unknown> => {
  const text = await response.text();
  if (!text) return undefined;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    return { message: text };
  }
};

const toErrorPayload = (
  value: unknown,
):
  | {
      message?: string;
      error?: string;
      code?: string | number;
      errors?: Record<string, string>;
    }
  | undefined =>
  typeof value === "object" && value !== null
    ? (value as {
        message?: string;
        error?: string;
        code?: string | number;
        errors?: Record<string, string>;
      })
    : undefined;

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const token = await getAccessToken();
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  if (options.body !== undefined)
    headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  let response: Response;
  const controller = options.signal ? undefined : new AbortController();
  const timeout = controller
    ? setTimeout(() => controller.abort(), 15000)
    : undefined;
  try {
    response = await fetch(`${apiConfig.baseUrl}${path}`, {
      ...options,
      headers,
      signal: options.signal ?? controller?.signal,
      body:
        options.body === undefined ? undefined : JSON.stringify(options.body),
    });
  } catch (cause) {
    if (cause instanceof Error && cause.name === "AbortError") {
      throw new ApiError(
        408,
        undefined,
        "Máy chủ phản hồi quá lâu. Vui lòng thử lại.",
      );
    }
    throw new ApiError(
      0,
      undefined,
      "Không thể kết nối máy chủ. Kiểm tra mạng và địa chỉ Gateway.",
    );
  } finally {
    if (timeout) clearTimeout(timeout);
  }

  const payload = await readJson(response);
  if (response.status === 401) {
    notifyUnauthorized();
  }
  if (!response.ok) {
    throw new ApiError(
      response.status,
      toErrorPayload(payload),
      `Máy chủ trả về lỗi ${response.status}.`,
    );
  }

  if (isEnvelope<T>(payload)) {
    if (payload.success === false) {
      throw new ApiError(
        response.status,
        payload,
        payload.message ?? "Yêu cầu không thành công.",
      );
    }
    return payload.data as T;
  }

  return payload as T;
}

export const apiGet = <T>(path: string, options?: ApiRequestOptions) =>
  apiRequest<T>(path, { ...options, method: "GET" });

export const apiPost = <T>(
  path: string,
  body: unknown,
  options?: ApiRequestOptions,
) => apiRequest<T>(path, { ...options, method: "POST", body });

export const apiPut = <T>(
  path: string,
  body: unknown,
  options?: ApiRequestOptions,
) => apiRequest<T>(path, { ...options, method: "PUT", body });

export const apiDelete = <T>(path: string, options?: ApiRequestOptions) =>
  apiRequest<T>(path, { ...options, method: "DELETE" });
