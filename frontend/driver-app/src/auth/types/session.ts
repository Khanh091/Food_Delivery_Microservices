export type AuthStatus =
  | "initializing"
  | "authenticated"
  | "unauthenticated"
  | "error";

export interface AuthUser {
  userId: string | null;
  username: string | null;
  email: string | null;
  firstName: string | null;
  lastName: string | null;
  displayName: string | null;
  roles: string[];
}

export interface StoredSession {
  accessToken: string;
  refreshToken?: string;
  idToken?: string;
  tokenType: string;
  expiresIn?: number;
  issuedAt: number;
  scope?: string;
}
