import {
  Component,
  effect,
  inject,
  Injector,
  OnInit,
  runInInjectionContext,
  signal,
} from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { catchError, finalize, forkJoin, map, of } from 'rxjs';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { SearchStore } from '../../utils/search/search-store';
import { DocumentDto } from '../../documents/documents.model';
import { DocumentService } from '../../documents/documents.service';

type DocumentShareState = 'loading' | 'available' | 'added';

interface GroupAddDocumentDialogData {
  groupId: number;
  groupName: string;
  onDocumentLinked?: () => void;
}

@Component({
  selector: 'app-group-add-document-dialog',
  standalone: false,
  templateUrl: './group-add-document-dialog.component.html',
  styleUrl: './group-add-document-dialog.component.scss',
})
export class GroupAddDocumentDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly injector = inject(Injector);
  private readonly config = inject(DynamicDialogConfig);
  private readonly ref = inject(DynamicDialogRef);
  private readonly messageService = inject(MessageService);
  private readonly documentService = inject(DocumentService);

  readonly limitOptions = [5, 10, 20];
  readonly page = signal(1);
  readonly size = signal(10);
  readonly documentStates = signal<Record<number, DocumentShareState>>({});
  readonly addingById = signal<Record<number, boolean>>({});

  readonly form = this.fb.group({
    titleSearch: [''],
    dateFrom: [null as Date | null],
    dateTo: [null as Date | null],
    ownedOnly: [true],
  });

  readonly groupId = (this.config.data as GroupAddDocumentDialogData).groupId;
  readonly groupName = (this.config.data as GroupAddDocumentDialogData).groupName;

  searchStore!: SearchStore<DocumentDto>;

  ngOnInit(): void {
    runInInjectionContext(this.injector, () => {
      this.searchStore = new SearchStore<DocumentDto>(
        this.form,
        (params) => this.documentService.find(params),
        this.page,
        this.size
      );

      effect(() => {
        const documents = this.searchStore.items().content;
        const states = this.documentStates();
        const uncheckedDocuments = documents.filter((document) => !states[document.id]);

        if (uncheckedDocuments.length === 0) {
          return;
        }

        this.documentStates.update((currentStates) => {
          const nextStates = { ...currentStates };
          uncheckedDocuments.forEach((document) => {
            nextStates[document.id] = 'loading';
          });
          return nextStates;
        });

        forkJoin(
          uncheckedDocuments.map((document) =>
            this.documentService.listAccess(document.id).pipe(
              map((accessEntries) => ({
                id: document.id,
                state: accessEntries.some((entry) => entry.groupId === this.groupId)
                  ? ('added' as const)
                  : ('available' as const),
              })),
              catchError(() =>
                of({
                  id: document.id,
                  state: 'available' as const,
                })
              )
            )
          )
        ).subscribe((results) => {
          this.documentStates.update((currentStates) => {
            const nextStates = { ...currentStates };
            results.forEach((result) => {
              nextStates[result.id] = result.state;
            });
            return nextStates;
          });
        });
      });
    });
  }

  onPageChange(page: number): void {
    if (page !== this.page()) {
      this.page.set(page);
    }
  }

  onSizeChange(size: number): void {
    if (size !== this.size()) {
      this.size.set(size);
    }
  }

  clearFilters(): void {
    this.form.reset({
      ownedOnly: true,
    });
    this.page.set(1);
  }

  close(): void {
    this.ref.close();
  }

  isAlreadyAdded(documentId: number): boolean {
    return this.documentStates()[documentId] === 'added';
  }

  isChecking(documentId: number): boolean {
    return this.documentStates()[documentId] === 'loading';
  }

  isAdding(documentId: number): boolean {
    return !!this.addingById()[documentId];
  }

  addDocument(document: DocumentDto): void {
    if (this.isAlreadyAdded(document.id) || this.isChecking(document.id) || this.isAdding(document.id)) {
      return;
    }

    this.addingById.update((current) => ({ ...current, [document.id]: true }));

    this.documentService
      .grantGroupAccess(document.id, this.groupId)
      .pipe(
        finalize(() => {
          this.addingById.update((current) => ({ ...current, [document.id]: false }));
        })
      )
      .subscribe({
        next: () => {
          this.documentStates.update((currentStates) => ({
            ...currentStates,
            [document.id]: 'added',
          }));
          this.messageService.add({
            severity: 'success',
            summary: 'Document added',
            detail: `"${document.title}" is now available in ${this.groupName}.`,
          });
          (this.config.data as GroupAddDocumentDialogData).onDocumentLinked?.();
        },
        error: (error) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Could not add document',
            detail: error.error?.message ?? 'Please try again.',
          });
        },
      });
  }
}