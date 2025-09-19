import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core"
import { AuthenticationRequest } from "./security.model";
import { Observable } from "rxjs";

@Injectable()
export class SecurityService {
  constructor(private httpClient: HttpClient) {}

  login(authRequest: AuthenticationRequest): Observable<void> {
    return this.httpClient.post<void>('api/auth/authenticate', authRequest);
  }
}