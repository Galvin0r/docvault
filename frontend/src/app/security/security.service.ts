import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core"
import { AuthenticationRequest, RegistrationRequest } from "./security.model";
import { Observable } from "rxjs";

@Injectable()
export class SecurityService {
  constructor(private httpClient: HttpClient) {}

  login(authRequest: AuthenticationRequest): Observable<void> {
    return this.httpClient.post<void>('api/auth/authenticate', authRequest);
  }

  register(regRequest: RegistrationRequest): Observable<void> {
    return this.httpClient.post<void>('api/auth/register', regRequest);
  }

  resendActivationCode(email: string): Observable<void> {
    return this.httpClient.post<void>('api/auth/resendActivation', null, {
      params: { email }
    });
  }

  activateAccount(token: number): Observable<void> {
    return this.httpClient.post<void>('api/auth/activateAccount', null, {
      params: { token }
    });
  }

  resetPassword(email: string): Observable<void> {
    return this.httpClient.post<void>('api/auth/resetPassword', null, {
      params: { email }
    });
  }

  setNewPassword(token: string, password: string): Observable<void> {
    return this.httpClient.post<void>('api/auth/setNewPassword', null, {
      params: { token, password }
    });
  }
}