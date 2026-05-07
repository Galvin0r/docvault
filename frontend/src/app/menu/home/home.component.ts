import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, finalize, of, startWith, switchMap, tap } from 'rxjs';
import { Page } from '../../app.model';
import {
  DocumentSearchMode,
  DocumentSearchResultDto,
  DocumentSearchScope,
} from '../../documents/documents.model';
import { DocumentService } from '../../documents/documents.service';
import { UserInfo } from '../../security/security.model';
import { DEBOUNCE_MS } from '../../utils/consts';

@Component({
  selector: 'app-home',
  standalone: false,
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private documentService = inject(DocumentService);

  userInfo: UserInfo | null = null;
  loading = signal(false);
  page = signal(1);
  size = signal(10);
  results = signal<Page<DocumentSearchResultDto>>({
    content: [],
    totalElements: 0,
    size: 10,
    number: 0,
  });

  modeOptions: { label: string; value: DocumentSearchMode }[] = [
    { label: 'Keyword', value: 'KEYWORD' },
    { label: 'Semantic', value: 'VECTOR' },
  ];

  scopeOptions: { label: string; value: DocumentSearchScope }[] = [
    { label: 'Public', value: 'PUBLIC' },
  ];

  readonly sizeOptions = [10, 20, 50];

  filtersForm = this.fb.group({
    mode: this.fb.nonNullable.control<DocumentSearchMode>('KEYWORD'),
    content: this.fb.nonNullable.control(''),
    title: this.fb.nonNullable.control(''),
    author: this.fb.nonNullable.control(''),
    uploadedFrom: this.fb.control<Date | null>(null),
    uploadedTo: this.fb.control<Date | null>(null),
    scope: this.fb.nonNullable.control<DocumentSearchScope>('PUBLIC'),
  });

  ngOnInit(): void {
    this.route.data
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ userInfo }) => {
        this.userInfo = userInfo ?? null;
        this.scopeOptions = this.userInfo
          ? [
              { label: 'All accessible', value: 'ACCESSIBLE' },
              { label: 'Public', value: 'PUBLIC' },
              { label: 'Owned by me', value: 'OWNED_BY_ME' },
              { label: 'Shared with me', value: 'SHARED_WITH_ME' },
            ]
          : [{ label: 'Public', value: 'PUBLIC' }];

        this.filtersForm.controls.scope.setValue(this.userInfo ? 'ACCESSIBLE' : 'PUBLIC');
      });

    this.applyQueryParams(this.route.snapshot.queryParamMap, false);

    this.filtersForm.valueChanges
      .pipe(
        startWith(this.filtersForm.getRawValue()),
        debounceTime(DEBOUNCE_MS),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        switchMap(() => {
          this.page.set(1);
          return this.fetchResults();
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe();

    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => this.applyQueryParams(params));
  }

  onPageChange(page: number) {
    if (page === this.page()) return;
    this.page.set(page);
    this.fetchResults().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
  }

  onSizeChange(size: number) {
    if (size === this.size()) return;
    this.size.set(size);
    this.page.set(1);
    this.fetchResults().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
  }

  download(documentId: number) {
    this.documentService.download(documentId).subscribe(url => {
      window.location.href = url;
    });
  }

  resetFilters() {
    this.filtersForm.reset({
      mode: 'KEYWORD',
      content: '',
      title: '',
      author: '',
      uploadedFrom: null,
      uploadedTo: null,
      scope: this.userInfo ? 'ACCESSIBLE' : 'PUBLIC',
    });
  }

  openDocument(documentId: number) {
    this.router.navigate(['/document', documentId]);
  }

  fileKind(result: DocumentSearchResultDto): string {
    const searchableText = [
      result.mimeType,
      result.originalFilename,
      result.title,
      result.contentSnippet,
      result.highlightedContentSnippet,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();

    if (searchableText.includes('application/pdf')) return 'PDF';
    if (searchableText.includes('text/plain') || searchableText.includes('text/')) return 'TXT';
    if (
      searchableText.includes('msword') ||
      searchableText.includes('wordprocessingml') ||
      searchableText.includes('officedocument.word') ||
      searchableText.includes('application/vnd.openxmlformats-officedocument')
    ) return 'DOC';
    if (searchableText.includes('epub')) return 'EPUB';

    const filename = result.originalFilename || result.title;
    const extension = filename.split('.').pop()?.toLowerCase();
    if (extension === 'pdf') return 'PDF';
    if (extension === 'txt') return 'TXT';
    if (extension === 'doc' || extension === 'docx') return 'DOC';
    if (extension === 'epub') return 'EPUB';
    return 'FILE';
  }

  fileKindClass(result: DocumentSearchResultDto): string {
    return `kind-${this.fileKind(result).toLowerCase()}`;
  }

  displayTitle(result: DocumentSearchResultDto): string {
    return this.highlightHtml(result.highlightedTitle, result.title);
  }

  displaySnippet(result: DocumentSearchResultDto): string {
    return this.highlightHtml(
      result.highlightedContentSnippet,
      result.contentSnippet || 'No matching fragment preview is available.',
    );
  }

  visibilityIcon(result: DocumentSearchResultDto): string {
    return result.visibility === 'PUBLIC' ? 'pi-lock-open' : 'pi-shield';
  }

  visibilityLabel(result: DocumentSearchResultDto): string {
    return result.visibility.replaceAll('_', ' ');
  }

  private fetchResults() {
    const params = this.queryParams();
    if (this.isBlankSemanticSearch(params)) {
      const emptyPage = this.emptyPage();
      this.results.set(emptyPage);
      this.loading.set(false);
      return of(emptyPage);
    }

    this.loading.set(true);
    return this.documentService.search(params).pipe(
      tap(page => {
        this.results.set(page);
      }),
      catchError(() => {
        const emptyPage = this.emptyPage();
        this.results.set(emptyPage);
        return of(emptyPage);
      }),
      finalize(() => this.loading.set(false)),
    );
  }

  private isBlankSemanticSearch(params: Record<string, any>): boolean {
    return params['mode'] === 'VECTOR' && !params['content'];
  }

  private emptyPage(): Page<DocumentSearchResultDto> {
    return {
      content: [],
      totalElements: 0,
      size: this.size(),
      number: this.page() - 1,
    };
  }

  private queryParams(): Record<string, any> {
    const value = this.filtersForm.getRawValue();
    const params: Record<string, any> = {
      mode: value.mode,
      scope: this.userInfo ? value.scope : 'PUBLIC',
      page: this.page() - 1,
      size: this.size(),
    };

    this.addTextParam(params, 'content', value.content);
    this.addTextParam(params, 'title', value.title);
    this.addTextParam(params, 'author', value.author);

    if (value.uploadedFrom) {
      const from = new Date(value.uploadedFrom);
      from.setHours(0, 0, 0, 0);
      params['uploadedFrom'] = from.toISOString();
    }

    if (value.uploadedTo) {
      const to = new Date(value.uploadedTo);
      to.setHours(23, 59, 59, 999);
      params['uploadedTo'] = to.toISOString();
    }

    return params;
  }

  private addTextParam(params: Record<string, any>, key: string, value: string) {
    const text = value.trim();
    if (text) params[key] = text;
  }

  private applyQueryParams(params: ParamMap, emitEvent = true) {
    const content = params.get('content') ?? '';
    if (content === this.filtersForm.controls.content.value) return;

    this.filtersForm.patchValue({
      content,
      mode: 'KEYWORD',
    }, { emitEvent });
  }

  private highlightHtml(value: string | null | undefined, fallback: string): string {
    return this.escapeHtml(value || fallback)
      .replaceAll('&lt;mark&gt;', '<mark>')
      .replaceAll('&lt;/mark&gt;', '</mark>');
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }
}