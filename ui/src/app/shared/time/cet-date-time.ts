import { DISPLAY_LOCALE, DISPLAY_TIME_ZONE } from '../../core/display-settings';

/**
 * Formats date and time in the configured Central European zone with the zone abbreviation, for
 * example "15 Jan 2026, 10:00 CET" or "15 Jul 2026, 10:00 CEST" (CON-10). The zone is set
 * explicitly, so the browser's own zone has no effect.
 */
const formatter = new Intl.DateTimeFormat(DISPLAY_LOCALE, {
  timeZone: DISPLAY_TIME_ZONE,
  year: 'numeric',
  month: 'short',
  day: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
  timeZoneName: 'short',
});

/**
 * Formats an ISO-8601 timestamp with offset (as the API delivers it, architecture Section 9.2) as
 * date and time in the display zone. Returns an empty string for a missing value and the input
 * unchanged if it is not a valid timestamp, so bad data stays visible instead of disappearing.
 */
export function formatCetDateTime(value: string | null | undefined): string {
  if (value == null || value === '') {
    return '';
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : formatter.format(date);
}
