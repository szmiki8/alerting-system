import { Pipe, PipeTransform } from '@angular/core';
import { formatCetDateTime } from './cet-date-time';

/** Template form of {@link formatCetDateTime}: `{{ subscriber.subscribedAt | cetDateTime }}`. */
@Pipe({ name: 'cetDateTime' })
export class CetDateTimePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return formatCetDateTime(value);
  }
}
