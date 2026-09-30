/**
 * Display settings of the UI (CON-10, AQ-02, OP-15).
 *
 * Times are always shown in the Central European zone the backend is configured with, never in the
 * browser's own zone. Change the zone here only together with the Core's configuration.
 */
export const DISPLAY_TIME_ZONE = 'Europe/Budapest';

/**
 * Locale for formatting dates and times. English UI (ASM-09); `en-GB` gives day-month-year order,
 * a 24-hour clock and the zone abbreviations CET and CEST.
 */
export const DISPLAY_LOCALE = 'en-GB';
