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

export type DocumentSearchMode = 'KEYWORD' | 'VECTOR';
export type DocumentSearchScope = 'ACCESSIBLE' | 'PUBLIC' | 'OWNED_BY_ME' | 'SHARED_WITH_ME';

export interface DocumentSearchResultDto {
    documentId: number;
    fragmentOrder: number | null;
    pageNumber: number | null;
    title: string;
    originalFilename: string | null;
    mimeType: string | null;
    size: number | null;
    highlightedTitle: string | null;
    contentSnippet: string | null;
    highlightedContentSnippet: string | null;
    uploadedAt: string;
    ownerId: number;
    ownerLogin: string;
    visibility: Visibility;
    score: number;
}

export interface DocumentContentFragmentDto {
    fragmentOrder: number | null;
    pageNumber: number | null;
    content: string;
}

export interface DocumentAccessDto {
    id: number;
    documentId: number;
    userId: number | null;
    userLogin: string | null;
    groupId: number | null;
    groupName: string | null;
}

export interface UploadStatus {
    id: string;
    filename: string;
    progress: number;
    state: 'UPLOADING' | 'UPLOADED' | 'FAILED' | 'CANCELLED';
    error?: string;
    subscription?: Subscription;
}