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
