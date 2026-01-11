import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'fileExtension',
    standalone: false
})
export class FileExtensionPipe implements PipeTransform {
    transform(filename: string | undefined): string {
        if (!filename) return 'FILE';
        const extension = filename.slice(((filename.lastIndexOf(".") - 1) >>> 0) + 2);
        return extension ? extension.toUpperCase() : 'FILE';
    }
}
