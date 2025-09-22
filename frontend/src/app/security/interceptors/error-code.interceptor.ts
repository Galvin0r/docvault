import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError, Observable, throwError } from "rxjs";
import { mapHttpErrorCode } from "../../utils/functions";

@Injectable()
export class ErrorCodeInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        const code = mapHttpErrorCode(error);
        if (code) (error as any).appCode = code;
        return throwError(() => error);
      })
    );
  }
}