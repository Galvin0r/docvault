import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core"

@Injectable()
export class SecurityService {
  constructor(private httpClient: HttpClient) {}
}