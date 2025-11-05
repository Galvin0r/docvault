import { FormBuilder } from "@angular/forms";
import { getDeviceId, serializeForm } from "./functions";

describe("getDeviceId", () => {
  let origStorage: Storage;

  beforeEach(() => {
    origStorage = window.localStorage;

    const store = new Map<string, string>();
    const mockStorage: Storage = {
      getItem: (k: string) => (store.has(k) ? store.get(k)! : null),
      setItem: (k: string, v: string) => { store.set(k, v); },
      removeItem: (k: string) => { store.delete(k); },
      clear: () => { store.clear(); },
      key: (i: number) => Array.from(store.keys())[i] ?? null,
      get length() { return store.size; },
    };

    spyOnProperty(window, "localStorage", "get").and.returnValue(mockStorage);
  });

  afterEach(() => {
    Object.defineProperty(window, "localStorage", { value: origStorage });
  });

  it("returns existing id from localStorage", () => {
    spyOn(window.localStorage, "getItem").and.returnValue("old-id");
    const uuidSpy = spyOn(window.crypto, "randomUUID");

    const id = getDeviceId();

    expect(id).toBe("old-id");
    expect(uuidSpy).not.toHaveBeenCalled();
  });

  it("when id is absent -> generates randomUUID, writes down and returns new id", () => {
    spyOn(window.localStorage, "getItem").and.returnValue(null);
    const uuidSpy = spyOn(window.crypto, "randomUUID").and.returnValue("00000000-0000-0000-0000-000000000000");
    const setSpy = spyOn(window.localStorage, "setItem");

    const id = getDeviceId();

    expect(id).toBe("00000000-0000-0000-0000-000000000000");
    expect(uuidSpy).toHaveBeenCalled();
    expect(setSpy).toHaveBeenCalledWith("docvault_device_id_v1", "00000000-0000-0000-0000-000000000000");
  });

  it("honors not standard storageKey", () => {
    spyOn(window.localStorage, "getItem").and.returnValue(null);
    spyOn(window.crypto, "randomUUID").and.returnValue("11111111-1111-1111-1111-111111111111");
    const setSpy = spyOn(window.localStorage, "setItem");

    const id = getDeviceId("custom_key");

    expect(id).toBe("11111111-1111-1111-1111-111111111111");
    expect(setSpy).toHaveBeenCalledWith("custom_key", "11111111-1111-1111-1111-111111111111");
  });

  it("does not throw when setItem throws error (try/catch), but returns id", () => {
    spyOn(window.localStorage, "getItem").and.returnValue(null);
    spyOn(window.crypto, "randomUUID").and.returnValue("22222222-2222-2222-2222-222222222222");
    spyOn(window.localStorage, "setItem").and.throwError("quota");

    const id = getDeviceId();

    expect(id).toBe("22222222-2222-2222-2222-222222222222");
  });
});

describe('serializeForm', () => {
  const fb = new FormBuilder();

  it('trims strings, omits empty/whitespace, keeps non-empty', () => {
    const form = fb.group({
      a: ['  hello  '],
      b: ['   '],
      c: ['world'],
    });

    const out = serializeForm(form);

    expect(out).toEqual({ a: 'hello', c: 'world' });
    expect(out.hasOwnProperty('b')).toBeFalse();
  });

  it('omits null and undefined', () => {
    const form = fb.group({
      n: [null],
      u: [undefined as any],
    });

    const out = serializeForm(form);

    expect(out).toEqual({});
  });

  it('keeps zero and false', () => {
    const form = fb.group({
      z: [0],
      f: [false],
      t: [true],
      num: [42],
    });

    const out = serializeForm(form);

    expect(out).toEqual({ z: 0, f: false, t: true, num: 42 });
  });

  it('serializes Date to ISO string', () => {
    const d = new Date('2025-01-02T03:04:05.000Z');
    const form = fb.group({
      date: [d],
    });

    const out = serializeForm(form);

    expect(out).toEqual({ date: d.toISOString() });
  });

  it('keeps arrays and objects (including empty array)', () => {
    const form = fb.group({
      arr: [[1, 2]],
      emptyArr: [[]],
      obj: [{ a: 1 }],
    });

    const out = serializeForm(form);

    expect(out).toEqual({ arr: [1, 2], emptyArr: [], obj: { a: 1 } });
  });

  it('handles mixed form correctly', () => {
    const d = new Date('2025-06-01T10:00:00.000Z');
    const form = fb.group({
      text: ['  x  '],
      emptyText: ['  '],
      nilVal: [null],
      undefVal: [undefined as any],
      zero: [0],
      False: [false],
      True: [true],
      num: [7],
      date: [d],
      arr: [[9]],
      obj: [{ k: 'v' }],
    });

    const out = serializeForm(form);

    expect(out).toEqual({
      text: 'x',
      zero: 0,
      False: false,
      True: true,
      num: 7,
      date: d.toISOString(),
      arr: [9],
      obj: { k: 'v' },
    });

    expect(out.hasOwnProperty('emptyText')).toBeFalse();
    expect(out.hasOwnProperty('nilVal')).toBeFalse();
    expect(out.hasOwnProperty('undefVal')).toBeFalse();
  });
});
