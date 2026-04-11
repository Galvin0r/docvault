import { Subscription } from 'rxjs';
import { Visibility } from '../groups/groups.model';

export type DocumentStatus = 'UPLOADING' | 'UPLOADED' | 'INDEXING' | 'INDEXED' | 'FAILED';

export interface DocumentDto {
    id: number;
    title: string;
    description: string | null;
    originalFilename: string;
    mimeType: string;
    uploadedAt: string;
    visibility: Visibility;
    ownerId: number;
    ownerLogin: string;
    size: number;
    status: DocumentStatus;
    attempts?: number;
    nextAttemptAt?: string;
}

export interface UploadStatus {
    id: string;
    filename: string;
    progress: number;
    state: 'UPLOADING' | 'UPLOADED' | 'FAILED' | 'CANCELLED';
    error?: string;
    subscription?: Subscription;
}
