import { HttpErrorResponse } from '@angular/common/http';
import { HttpErrorCode } from './consts';
import { FormGroup } from '@angular/forms';
import { isNotNil, isNotEmpty, trim } from 'ramda';

export function getDeviceId(storageKey = 'docvault_device_id_v1'): string {
  let id = localStorage.getItem(storageKey);
  if (!id) {
    id = crypto.randomUUID();
    try {
      localStorage.setItem(storageKey, id);
    } catch {}
  }
  return id;
}

export function mapHttpErrorCode(err: HttpErrorResponse): HttpErrorCode | null {
  const src = err?.error;
  const text =
    typeof src === 'string' ? src : src?.error ?? src?.message ?? src?.detail ?? src?.title ?? '';
  const msg = String(text || '').toLowerCase();
  console.log(msg);
  if (msg.includes('invalid activation token')) return 'ATI';
  if (msg.includes('activation token has expired')) return 'ATE';
  if (msg.includes('account is not activated')) return 'NAA';
  if (msg.includes('user not found')) return 'UNF';
  if (msg.includes('invalid password activation token')) return 'PRTI';
  if (msg.includes('password reset token has expired')) return 'PRTE';
  if (msg.includes('user with email')) return 'DE';
  if (msg.includes('user with login')) return 'DU';
  if (msg.includes('bad credentials')) return 'IC';

  return null;
}

export function serializeForm(form: FormGroup): Record<string, any> {
  const query: Record<string, any> = {};
  for (const [name, ctrl] of Object.entries(form.controls)) {
    let value = ctrl.value;

    if (isNotNil(value)) {
      if (typeof value === 'string') {
        const text = trim(value);
        if (isNotEmpty(text)) {
          query[name] = text;
        }
      } else if (value instanceof Date) {
        query[name] = value.toISOString();
      } else {
        query[name] = value;
      }
    }
  }

  return query;
}
