import { Component, inject, signal } from '@angular/core';
import { DocumentService } from '../documents.service';

@Component({
    selector: 'app-upload-progress',
    standalone: false,
    templateUrl: './upload-progress.component.html',
    styleUrl: './upload-progress.component.scss',
})
export class UploadProgressComponent {
    documentService = inject(DocumentService);
    isMinimized = signal(false);

    get uploads() {
        return this.documentService.uploads;
    }

    toggleMinimize() {
        this.isMinimized.update(v => !v);
    }

    cancelUpload(uploadId: string, event: Event) {
        event.stopPropagation();
        this.documentService.cancelUpload(uploadId);
    }

    removeUpload(uploadId: string, event: Event) {
        event.stopPropagation();
        this.documentService.removeUpload(uploadId);
    }

    clearAll() {
        const uploadsToRemove = this.uploads().filter(u => u.state !== 'UPLOADING' || u.progress === 100);
        uploadsToRemove.forEach(u => this.documentService.removeUpload(u.id));
    }

    hasCompletedUploads(): boolean {
        return this.uploads().some(u => u.state !== 'UPLOADING' || u.progress === 100);
    }
}
