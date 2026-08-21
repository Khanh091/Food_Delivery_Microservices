import { getUsableAccessToken } from "../api/sessionManager";

type AccessTokenReader = () => Promise<string | null>;
type UnauthorizedHandler = () => void;

let accessTokenReader: AccessTokenReader = getUsableAccessToken;
let unauthorizedHandler: UnauthorizedHandler = () => undefined;

export const configureAuthSession = (config: {
  getAccessToken: AccessTokenReader;
  onUnauthorized: UnauthorizedHandler;
}) => {
  accessTokenReader = config.getAccessToken;
  unauthorizedHandler = config.onUnauthorized;
};

export const getAccessToken = () => accessTokenReader();

export const notifyUnauthorized = () => unauthorizedHandler();
