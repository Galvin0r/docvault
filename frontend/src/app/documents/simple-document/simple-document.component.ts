import { Component, input } from '@angular/core';
import { DocumentDto } from '../documents.model';

@Component({
  selector: 'app-simple-document',
  standalone: false,
  templateUrl: './simple-document.component.html',
  styleUrl: './simple-document.component.scss'
})
export class SimpleDocumentComponent {
  document = input.required<DocumentDto>();

  getFileIcon(): string {
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
  }
}
