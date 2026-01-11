import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UserInfo } from '../../security/security.model';
import { MenuItem } from 'primeng/api';
import { SecurityService } from '../../security/security.service';
import { DialogService } from 'primeng/dynamicdialog';
import { MessageService } from 'primeng/api';
import { UploadDialogComponent } from '../../documents/upload-dialog/upload-dialog.component';

@Component({
  selector: 'app-top-menu',
  standalone: false,
  templateUrl: './top-menu.component.html',
  styleUrl: './top-menu.component.scss',
})
export class TopMenuComponent {
  router = inject(Router);
  activatedRoute = inject(ActivatedRoute);
  userInfo: UserInfo | null = null;
  securityService = inject(SecurityService);

  menuItems: MenuItem[] = [
    { separator: true },
    {
      label: 'Profile',
      icon: 'pi  pi-user',
      command: () => this.router.navigate(['/user', this.userInfo?.login])

    },
    {
      label: 'Groups',
      icon: 'pi pi-users',
      routerLink: ['/groups']
    },
    { separator: true },
    {
      label: 'Log out',
      icon: 'pi pi-sign-out',
      command: () => this.logout(),
    },
  ];

  constructor() {
    this.activatedRoute.data.subscribe(({ userInfo }) => {
      this.userInfo = userInfo;
    });
  }

  home() {
    this.router.navigate(['']);
  }

  logout() {
    this.securityService.logout().subscribe(() => {
      location.reload();
    })
  }

  private dialogService = inject(DialogService);
  private messageService = inject(MessageService);

  openUploadDialog() {
    if (!this.userInfo) {
      this.messageService.add({
        severity: 'info',
        summary: 'Authentication Required',
        detail: 'Please sign in to upload documents.',
      });
      return;
    }

    this.dialogService.open(UploadDialogComponent, {
      header: 'Upload Document',
      width: '500px',
      modal: true,
      dismissableMask: true,
      breakpoints: {
        '960px': '75vw',
        '640px': '90vw'
      },
    });
  }
}
