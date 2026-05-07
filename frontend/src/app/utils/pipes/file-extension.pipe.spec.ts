import { FileExtensionPipe } from './file-extension.pipe';

describe('FileExtensionPipe', () => {
    let pipe: FileExtensionPipe;

    beforeEach(() => {
        pipe = new FileExtensionPipe();
    });

    it('should return FILE if filename is undefined or empty', () => {
        expect(pipe.transform(undefined)).toBe('FILE');
        expect(pipe.transform('')).toBe('FILE');
    });

    it('should extract extension and convert to uppercase', () => {
        expect(pipe.transform('document.pdf')).toBe('PDF');
        expect(pipe.transform('image.png')).toBe('PNG');
        expect(pipe.transform('archive.tar.gz')).toBe('GZ');
    });

    it('should return FILE if no extension present', () => {
        expect(pipe.transform('README')).toBe('FILE');
        expect(pipe.transform('file.')).toBe('FILE');
    });

    it('should handle filenames starts with dot', () => {
        expect(pipe.transform('.gitignore')).toBe('GITIGNORE');
    });
});