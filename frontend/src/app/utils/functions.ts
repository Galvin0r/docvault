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