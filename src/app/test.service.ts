import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

@Injectable()
export class TestService {
  constructor(private httpClient: HttpClient){}

  public test(): Observable<string> {
    return this.httpClient.get('/api/test', { responseType: 'text'});
  }

  public logout(): Observable<void> {
    return this.httpClient.post<void>('/api/account/logout', null);
  }
}