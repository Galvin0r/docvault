import { Component, inject } from '@angular/core';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';
import { DocumentDto } from '../documents.model';

@Component({
    selector: 'app-document-info-dialog',
    standalone: false,
    templateUrl: './document-info-dialog.component.html',
    styleUrl: './document-info-dialog.component.scss'
})
export class DocumentInfoDialogComponent {
    config = inject(DynamicDialogConfig);
    document: DocumentDto = this.config.data.document;
    currentUserLogin: string = this.config.data.currentUserLogin;

    isOwner = this.document.ownerLogin === this.currentUserLogin;

    showIndexingInfo = this.isOwner && (this.document.status === 'INDEXING' || this.document.status === 'FAILED');

    editTitle() {
        // TODO: implement title editing
        console.log('Edit title TODO');
    }

    editDescription() {
        // TODO: implement description editing
        console.log('Edit description TODO');
    }
}
