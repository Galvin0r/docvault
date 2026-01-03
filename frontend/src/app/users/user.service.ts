import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { UserInfo } from "../security/security.model";

@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private httpClient: HttpClient) {}

  changeLogin(newLogin: string): Observable<UserInfo> {
    return this.httpClient.patch<UserInfo>('/api/accounts/change-login', null, {
      params: { newLogin }
    });
  }
}