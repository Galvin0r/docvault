import { Pipe, PipeTransform } from '@angular/core';
import { Term } from '../consts';

@Pipe({ name: 'code', standalone: false })
export class CodePipe implements PipeTransform {
  transform(code: string | null | undefined, values: Term[]) {
    if (!code) return '';
    return values.find((t: Term) => t.code === code)?.name ?? '';
  }
}
