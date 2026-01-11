import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'fileExtension',
    standalone: false
})
export class FileExtensionPipe implements PipeTransform {
    transform(filename: string | undefined): string {
        if (!filename) return 'FILE';
        const lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex === -1) return 'FILE';
        const extension = filename.slice(lastDotIndex + 1);
        return extension ? extension.toUpperCase() : 'FILE';
    }
}
