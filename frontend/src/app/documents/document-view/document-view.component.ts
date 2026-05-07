import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, Validators } from '@angular/forms';
import { forkJoin, finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { GroupMembership, Visibility } from '../../groups/groups.model';
import { GroupService } from '../../groups/groups.service';
import { UserInfo } from '../../security/security.model';
import { DocumentAccessDto, DocumentContentFragmentDto, DocumentDto } from '../documents.model';
import { DocumentService } from '../documents.service';

type DocumentVisibility = 'PUBLIC' | 'PRIVATE';

@Component({
  selector: 'app-document-view',
  standalone: false,
  templateUrl: './document-view.component.html',
  styleUrl: './document-view.component.scss'
})
export class DocumentViewComponent {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private documentService = inject(DocumentService);
  private groupService = inject(GroupService);
  private messageService = inject(MessageService);

  document = signal<DocumentDto | null>(null);
  fragments = signal<DocumentContentFragmentDto[]>([]);
  accessEntries = signal<DocumentAccessDto[]>([]);
  groupMemberships = signal<GroupMembership[]>([]);
  groupTotal = signal(0);
  groupPage = signal(1);
  groupSize = signal(5);
  currentUser = signal<UserInfo | null>(null);
  loading = signal(false);
  saving = signal(false);
  sharingUser = signal(false);
  sharingGroup = signal(false);
  deleting = signal(false);
  editDialogVisible = signal(false);
  groupShareDialogVisible = signal(false);

  visibilityOptions = [
    { label: 'Private', value: 'PRIVATE' as DocumentVisibility },
    { label: 'Public', value: 'PUBLIC' as DocumentVisibility },
  ];

  editForm = this.fb.group({
    title: ['', [Validators.required]],
    visibility: ['PRIVATE' as DocumentVisibility],
  });

  shareForm = this.fb.group({
    userLogin: [''],
    groupName: [''],
  });

  isOwner = computed(() => {
    const doc = this.document();
    const user = this.currentUser();
    return !!doc && !!user && (doc.ownerId === user.id || doc.ownerLogin === user.login);
  });

  userAccess = computed(() => this.accessEntries().filter(entry => entry.userId != null));
  groupAccess = computed(() => this.accessEntries().filter(entry => entry.groupId != null));
  sharedGroupIds = computed(() => new Set(this.groupAccess().map(entry => entry.groupId)));

  constructor() {
    const routeTree = (this.route.pathFromRoot?.length
      ? this.route.pathFromRoot
      : [this.route.parent, this.route].filter((route): route is ActivatedRoute => route != null))
      .filter(route => route.data != null);
    routeTree.forEach(route => {
      route.data.subscribe(({ userInfo }) => {
        if (userInfo !== undefined) {
          this.currentUser.set(userInfo ?? null);
          this.refreshAccess();
        }
      });
    });
    this.route.paramMap.subscribe(params => {
      const documentId = Number(params.get('id'));
      if (Number.isFinite(documentId)) {
        this.load(documentId);
      }
    });
  }

  private load(documentId: number) {
    this.loading.set(true);
    forkJoin({
      document: this.documentService.get(documentId),
      fragments: this.documentService.getFragments(documentId, 2),
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ document, fragments }) => {
        this.document.set(document);
        this.fragments.set(fragments);
        this.editForm.reset({
          title: document.title,
          visibility: document.visibility === 'PUBLIC' ? 'PUBLIC' : 'PRIVATE',
        });
        this.refreshAccess();
      },
      error: () => this.messageService.add({
        severity: 'error',
        summary: 'Document not available',
        detail: 'Could not open this document.'
      }),
    });
  }

  private loadAccess(documentId: number) {
    this.documentService.listAccess(documentId).subscribe({
      next: entries => this.accessEntries.set(entries),
      error: () => this.messageService.add({
        severity: 'error',
        summary: 'Access list unavailable',
        detail: 'Could not load sharing information.'
      }),
    });
  }

  private refreshAccess() {
    const doc = this.document();
    if (!doc) return;

    if (this.isOwner()) {
      this.loadAccess(doc.id);
    } else {
      this.accessEntries.set([]);
    }
  }

  searchGroups(page = this.groupPage()) {
    const user = this.currentUser();
    if (!user) return;

    this.groupPage.set(page);
    this.groupService.findMemberships({
      userLogin: user.login,
      groupName: this.shareForm.controls.groupName.value?.trim() ?? '',
      page: page - 1,
      size: this.groupSize(),
    }).subscribe({
      next: page => {
        this.groupMemberships.set(page.content);
        this.groupTotal.set(page.totalElements);
      },
      error: () => {
        this.groupMemberships.set([]);
        this.groupTotal.set(0);
      },
    });
  }

  openGroupShareDialog() {
    this.groupShareDialogVisible.set(true);
    this.searchGroups(1);
  }

  openEditDialog() {
    const doc = this.document();
    if (!doc) return;

    this.editForm.reset({
      title: doc.title,
      visibility: doc.visibility === 'PUBLIC' ? 'PUBLIC' : 'PRIVATE',
    });
    this.editDialogVisible.set(true);
  }

  saveDetails() {
    const doc = this.document();
    if (!doc || this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const nextTitle = this.editForm.controls.title.value?.trim() ?? '';
    const nextVisibility = this.editForm.controls.visibility.value as Visibility;
    const calls = [];
    if (nextTitle && nextTitle !== doc.title) {
      calls.push(this.documentService.updateTitle(doc.id, nextTitle));
    }
    if (nextVisibility !== doc.visibility) {
      calls.push(this.documentService.updateVisibility(doc.id, nextVisibility));
    }

    if (!calls.length) return;

    this.saving.set(true);
    forkJoin(calls).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.messageService.add({ severity: 'success', summary: 'Saved', detail: 'Document details updated.' });
        this.editDialogVisible.set(false);
        this.load(doc.id);
      },
    });
  }

  shareWithUser() {
    const doc = this.document();
    const login = this.shareForm.controls.userLogin.value?.trim();
    if (!doc || !login) return;

    this.sharingUser.set(true);
    this.documentService.grantUserAccessByLogin(doc.id, login)
      .pipe(finalize(() => this.sharingUser.set(false)))
      .subscribe({
        next: () => {
          this.shareForm.patchValue({ userLogin: '' });
          this.loadAccess(doc.id);
        },
      });
  }

  revokeUser(userId: number | null) {
    const doc = this.document();
    if (!doc || userId == null) return;
    this.documentService.revokeUserAccess(doc.id, userId).subscribe(() => this.loadAccess(doc.id));
  }

  shareWithGroup(groupId: number) {
    const doc = this.document();
    if (!doc || this.isGroupShared(groupId)) return;

    this.sharingGroup.set(true);
    this.documentService.grantGroupAccess(doc.id, groupId)
      .pipe(finalize(() => this.sharingGroup.set(false)))
      .subscribe({
        next: () => {
          this.groupShareDialogVisible.set(false);
          this.loadAccess(doc.id);
        },
      });
  }

  isGroupShared(groupId: number | null) {
    return groupId != null && this.sharedGroupIds().has(groupId);
  }

  onGroupPageChange(page: number) {
    if (page !== this.groupPage()) {
      this.searchGroups(page);
    }
  }

  onGroupSizeChange(size: number) {
    if (size !== this.groupSize()) {
      this.groupSize.set(size);
      this.searchGroups(1);
    }
  }

  revokeGroup(groupId: number | null) {
    const doc = this.document();
    if (!doc || groupId == null) return;
    this.documentService.revokeGroupAccess(doc.id, groupId).subscribe(() => this.loadAccess(doc.id));
  }

  download() {
    const doc = this.document();
    if (!doc) return;
    this.documentService.download(doc.id).subscribe(url => window.open(url, '_blank'));
  }

  deleteDocument() {
    const doc = this.document();
    if (!doc) return;
    this.deleting.set(true);
    this.documentService.delete(doc.id)
      .pipe(finalize(() => this.deleting.set(false)))
      .subscribe(() => this.router.navigate(['/']));
  }

  openOwner() {
    const ownerLogin = this.document()?.ownerLogin;
    if (ownerLogin) this.router.navigate(['/user', ownerLogin]);
  }

  fileKind(document: DocumentDto): string {
    const mimeType = document.mimeType ?? '';
    const fileName = document.originalFilename ?? '';
    if (mimeType === 'application/pdf' || fileName.endsWith('.pdf')) return 'PDF';
    if (mimeType.includes('word') || mimeType.includes('document') || fileName.match(/\.docx?$/i)) return 'DOC';
    if (mimeType.startsWith('text/') || fileName.endsWith('.txt')) return 'TXT';
    if (mimeType.includes('epub') || fileName.endsWith('.epub')) return 'EPUB';
    return 'FILE';
  }

  fileKindClass(document: DocumentDto): string {
    return `kind-${this.fileKind(document).toLowerCase()}`;
  }
}