export interface AuthenticationRequest {
  email: string | null,
  password: string,
  login: string | null,
  deviceInfo: string,
  rememberMe: boolean
}

export interface RegistrationRequest {
  email: string,
  password: string,
  login: string;
}

export interface UserInfo {
  id: number;
  login: string,
  email: string,
  created: string
}
