import { Component, input, inject, output, computed, signal } from '@angular/core';
import { DocumentDto } from '../documents.model';
import { DocumentService } from '../documents.service';
import { DialogService } from 'primeng/dynamicdialog';
import { DocumentInfoDialogComponent } from '../document-info-dialog/document-info-dialog.component';

@Component({
  selector: 'app-simple-document',
  standalone: false,
  templateUrl: './simple-document.component.html',
  styleUrl: './simple-document.component.scss'
})
export class SimpleDocumentComponent {
  document = input.required<DocumentDto>();
  currentUserLogin = input.required<string>();
  deleted = output<number>();
  deleting = signal(false);
  private documentService = inject(DocumentService);
  private dialogService = inject(DialogService);

  isOwner = computed(() => this.document().ownerLogin === this.currentUserLogin());

  fileIcon = computed(() => {
    const mimeType = this.document().mimeType;
    if (mimeType.startsWith('image/')) return 'pi-image';
    if (mimeType.startsWith('video/')) return 'pi-video';
    if (mimeType.startsWith('audio/')) return 'pi-volume-up';
    if (mimeType === 'application/pdf') return 'pi-file-pdf';
    if (mimeType.includes('word') || mimeType.includes('document')) return 'pi-file-word';
    if (mimeType.includes('sheet') || mimeType.includes('excel')) return 'pi-file-excel';
    if (mimeType.includes('presentation') || mimeType.includes('powerpoint')) return 'pi-file';
    if (mimeType.includes('zip') || mimeType.includes('archive') || mimeType.includes('compressed')) return 'pi-folder';
    if (mimeType.startsWith('text/')) return 'pi-file-edit';
    return 'pi-file';
  });

  openInfo() {
    this.dialogService.open(DocumentInfoDialogComponent, {
      header: 'Document Information',
      width: '500px',
      modal: true,
      closable: true,
      dismissableMask: true,
      data: {
        document: this.document(),
        currentUserLogin: this.currentUserLogin()
      }
    });
  }

  download() {
    this.documentService.download(this.document().id).subscribe(url => {
      window.open(url, '_blank');
    });
  }

  delete() {
    this.deleting.set(true);
    this.documentService.delete(this.document().id).subscribe({
      next: () => {
        this.deleted.emit(this.document().id);
        this.deleting.set(false);
      },
      error: () => {
        this.deleting.set(false);
      }
    });
  }
}
