export interface AuthenticationRequest {
  email: string,
  password: string,
  login: string,
  deviceInfo: string,
  rememberMe: boolean
}

export interface UserInfo {
  login: string,
  email: string,
  roles: string[]
}