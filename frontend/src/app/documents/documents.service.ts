import { Injectable, signal, WritableSignal } from '@angular/core';
import { HttpClient, HttpEventType, HttpHeaders, HttpParams, HttpRequest } from '@angular/common/http';
import { Visibility } from '../groups/groups.model';
import { Observable, catchError, map, of, switchMap } from 'rxjs';
import { DocumentDto, UploadStatus } from './documents.model';
import { MessageService } from 'primeng/api';
import { Page } from '../app.model';

@Injectable({
    providedIn: 'root',
})
export class DocumentService {
    constructor(private http: HttpClient, private messageService: MessageService) { }

    private uploadsSignal: WritableSignal<UploadStatus[]> = signal([]);
    readonly uploads = this.uploadsSignal.asReadonly();

    find(params: Record<string, any>): Observable<Page<DocumentDto>> {
        return this.http.get<Page<DocumentDto>>('/api/documents', { params });
    }

    createDraft(title: string, description: string | null, visibility: Visibility): Observable<number> {
        const params = new HttpParams()
            .set('title', title)
            .set('visibility', visibility)
            .set('description', description || '');
        return this.http.post<number>('api/documents/draft', null, { params });
    }

    uploadFile(documentId: number, file: File) {
        const uploadId = crypto.randomUUID();
        const newUpload: UploadStatus = {
            id: uploadId,
            filename: file.name,
            progress: 0,
            state: 'UPLOADING',
        };

        this.addUpload(newUpload);

        const contentType = file.type || 'application/octet-stream';
        const subscription = this.signUpload(documentId, file.type || 'application/octet-stream', file.name)
            .pipe(
                switchMap(signedUrl => {
                    const req = new HttpRequest('PUT', signedUrl, file, {
                        reportProgress: true,
                        headers: new HttpHeaders({ 'Content-Type': contentType }),
                    });
                    return this.http.request(req).pipe(
                        map(event => ({ event, documentId: documentId, signedUrl }))
                    );
                }),
                switchMap(data => {
                    const { event } = data;
                    if (event.type === HttpEventType.UploadProgress) {
                        const progress = Math.round((100 * event.loaded) / (event.total || 1));
                        this.updateUpload(uploadId, { progress, state: 'UPLOADING' });
                        return of(null);
                    } else if (event.type === HttpEventType.Response) {
                        return this.completeUpload(documentId);
                    }
                    return of(null);
                }),
                catchError(error => {
                    const errorMsg = error.error?.message || error.message || 'Upload failed';
                    this.updateUpload(uploadId, { state: 'FAILED', error: errorMsg });
                    this.messageService.add({ severity: 'error', summary: 'Upload Failed', detail: errorMsg });
                    return of(null);
                })
            ).subscribe(result => {
                if (result !== null) {
                    this.updateUpload(uploadId, { progress: 100, state: 'UPLOADED' });
                    this.messageService.add({ severity: 'success', summary: 'Upload Completed', detail: 'File uploaded successfully' });
                }
            });

        this.updateUpload(uploadId, { subscription });
    }

    signUpload(documentId: number, contentType: string, originalFilename: string): Observable<string> {
        const params = new HttpParams().set('contentType', contentType).set('originalFilename', originalFilename);
        return this.http.post(`api/documents/${documentId}/sign-upload`, null, { params, responseType: 'text' });
    }

    completeUpload(documentId: number): Observable<void> {
        return this.http.post<void>(`api/documents/${documentId}/complete-upload`, {});
    }

    cancelUpload(uploadId: string) {
        const upload = this.uploadsSignal().find(u => u.id === uploadId);
        if (upload && upload.subscription) {
            upload.subscription.unsubscribe();
            this.updateUpload(uploadId, { state: 'CANCELLED', progress: 0 });
        }
    }

    removeUpload(uploadId: string) {
        this.uploadsSignal.update(uploads => uploads.filter(u => u.id !== uploadId));
    }

    delete(documentId: number): Observable<void> {
        return this.http.delete<void>(`/api/documents/delete/${documentId}`);
    }

    download(documentId: number): Observable<string> {
        return this.http.get(`/api/documents/download/${documentId}`, { responseType: 'text' });
    }

    private addUpload(upload: UploadStatus) {
        this.uploadsSignal.update(uploads => [...uploads, upload]);
    }

    private updateUpload(id: string, update: Partial<UploadStatus>) {
        this.uploadsSignal.update(uploads =>
            uploads.map(u => (u.id === id ? { ...u, ...update } : u))
        );
    }
}
