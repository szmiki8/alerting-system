import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { formatCetDateTime } from './cet-date-time';
import { CetDateTimePipe } from './cet-date-time.pipe';

describe('formatCetDateTime', () => {
  afterEach(() => vi.unstubAllEnvs());

  it('formats a winter time as CET', () => {
    expect(formatCetDateTime('2026-01-15T10:00:00+01:00')).toBe('15 Jan 2026, 10:00 CET');
  });

  it('formats a summer time as CEST', () => {
    expect(formatCetDateTime('2026-07-15T10:00:00+02:00')).toBe('15 Jul 2026, 10:00 CEST');
  });

  it('converts a timestamp with another offset into the display zone', () => {
    expect(formatCetDateTime('2026-07-15T08:00:00Z')).toBe('15 Jul 2026, 10:00 CEST');
  });

  it('handles the spring daylight-saving transition (02:00 CET becomes 03:00 CEST)', () => {
    expect(formatCetDateTime('2026-03-29T01:59:00+01:00')).toBe('29 Mar 2026, 01:59 CET');
    expect(formatCetDateTime('2026-03-29T03:00:00+02:00')).toBe('29 Mar 2026, 03:00 CEST');
  });

  it('tells the repeated hour apart on the autumn transition day', () => {
    expect(formatCetDateTime('2026-10-25T02:30:00+02:00')).toBe('25 Oct 2026, 02:30 CEST');
    expect(formatCetDateTime('2026-10-25T02:30:00+01:00')).toBe('25 Oct 2026, 02:30 CET');
  });

  it.each(['UTC', 'America/New_York', 'Asia/Tokyo'])('ignores the browser zone (%s)', (zone) => {
    vi.stubEnv('TZ', zone);
    // Proves the zone switch took effect: 08:00Z is a different local hour in every zone above.
    expect(new Date('2026-07-15T08:00:00Z').getHours()).not.toBe(10);
    expect(formatCetDateTime('2026-07-15T10:00:00+02:00')).toBe('15 Jul 2026, 10:00 CEST');
    expect(formatCetDateTime('2026-01-15T10:00:00+01:00')).toBe('15 Jan 2026, 10:00 CET');
  });

  it('returns an empty string for a missing value', () => {
    expect(formatCetDateTime(null)).toBe('');
    expect(formatCetDateTime(undefined)).toBe('');
    expect(formatCetDateTime('')).toBe('');
  });

  it('returns an unparsable value unchanged', () => {
    expect(formatCetDateTime('not a date')).toBe('not a date');
  });
});

describe('CetDateTimePipe', () => {
  @Component({
    imports: [CetDateTimePipe],
    template: `<time>{{ value | cetDateTime }}</time>`,
    changeDetection: ChangeDetectionStrategy.OnPush,
  })
  class Host {
    value = '2026-01-15T10:00:00+01:00';
  }

  it('formats the value in a template', async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    expect((fixture.nativeElement as HTMLElement).textContent).toBe('15 Jan 2026, 10:00 CET');
  });
});
