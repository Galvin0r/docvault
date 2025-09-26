import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AuthenticationRequest, RegistrationRequest, UserInfo } from './security.model';
import { catchError, Observable, of } from 'rxjs';
import { ResolveFn } from '@angular/router';

@Injectable({ providedIn: 'root' })
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
      params: { email },
    });
  }

  activateAccount(token: number): Observable<void> {
    return this.httpClient.post<void>('api/auth/activateAccount', null, {
      params: { token },
    });
  }

  resetPassword(email: string): Observable<void> {
    return this.httpClient.post<void>('api/auth/resetPassword', null, {
      params: { email },
    });
  }

  setNewPassword(token: string, password: string): Observable<void> {
    return this.httpClient.post<void>('api/auth/setNewPassword', null, {
      params: { token, password },
    });
  }

  getUserInfo(): Observable<UserInfo> {
    return this.httpClient.get<UserInfo>('/api/account/me');
  }

  logout(): Observable<void> {
    return this.httpClient.post<void>('/api/account/logout', null);
  }
}

export const userResolver: ResolveFn<UserInfo | null> = () =>
  inject(SecurityService)
    .getUserInfo()
    .pipe(catchError(() => of(null)));
