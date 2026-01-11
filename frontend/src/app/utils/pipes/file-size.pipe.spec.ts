import { FileSizePipe } from './file-size.pipe';

describe('FileSizePipe', () => {
    let pipe: FileSizePipe;

    beforeEach(() => {
        pipe = new FileSizePipe();
    });

    it('should format 0 as 0 B', () => {
        expect(pipe.transform(0)).toBe('0 B');
    });

    it('should format bytes correctly', () => {
        expect(pipe.transform(512)).toBe('512 B');
    });

    it('should format KB correctly', () => {
        expect(pipe.transform(1024)).toBe('1 KB');
        expect(pipe.transform(2048)).toBe('2 KB');
        expect(pipe.transform(1536)).toBe('1.5 KB');
    });

    it('should format MB correctly', () => {
        expect(pipe.transform(1024 * 1024)).toBe('1 MB');
        expect(pipe.transform(1024 * 1024 * 5.5)).toBe('5.5 MB');
    });

    it('should format GB correctly', () => {
        expect(pipe.transform(1024 * 1024 * 1024)).toBe('1 GB');
        expect(pipe.transform(1024 * 1024 * 1024 * 2.1)).toBe('2.1 GB');
    });
});
