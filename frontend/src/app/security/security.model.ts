export interface AuthenticationRequest {
  email: string,
  password: string,
  login: string,
  deviceInfo: string,
  rememberMe: boolean
}

export interface RegistrationRequest {
  email: string,
  password: string,
  login: string;
}

export interface UserInfo {
  login: string,
  email: string
}