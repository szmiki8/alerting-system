// WCAG 2.1 AA contrast check of the built Angular Material theme (FE-04, NFR-13).
// Reads the system colour tokens (--mat-sys-*) from the production stylesheet in dist/ and
// fails when a text or UI component pair is below its minimum ratio.
// Run after `npm run build`: `npm run check:contrast`.
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const dir = 'dist/alerting-ui/browser';
const file = readdirSync(dir).find((name) => /^styles-.*\.css$/.test(name));
if (!file) {
  console.error(`No styles-*.css in ${dir}; run "npm run build" first.`);
  process.exit(1);
}
const css = readFileSync(join(dir, file), 'utf8');

function token(name) {
  const match = css.match(new RegExp(`--mat-sys-${name}:\\s*(#[0-9a-fA-F]{3,6})\\b`));
  if (!match) throw new Error(`Colour token --mat-sys-${name} not found`);
  return match[1];
}

function luminance(hex) {
  let h = hex.slice(1);
  if (h.length === 3) h = [...h].map((c) => c + c).join('');
  const [r, g, b] = [0, 2, 4].map((i) => {
    const c = parseInt(h.slice(i, i + 2), 16) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function ratio(a, b) {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

// [foreground, background, minimum]: 4.5 for normal text, 3 for UI components and focus indicators.
const pairs = [
  ['on-surface', 'surface', 4.5],
  ['on-surface-variant', 'surface', 4.5],
  ['primary', 'surface', 4.5],
  ['on-primary', 'primary', 4.5],
  ['on-primary-container', 'primary-container', 4.5],
  ['on-secondary-container', 'secondary-container', 4.5],
  ['error', 'surface', 4.5],
  ['on-error', 'error', 4.5],
  ['on-error-container', 'error-container', 4.5],
  ['on-surface', 'surface-container-highest', 4.5],
  ['on-surface-variant', 'surface-container-highest', 4.5],
  ['outline', 'surface', 3],
  ['primary', 'surface-container', 3],
];

let failed = false;
for (const [fg, bg, min] of pairs) {
  const value = ratio(token(fg), token(bg));
  const ok = value >= min;
  failed ||= !ok;
  console.log(
    `${ok ? 'PASS' : 'FAIL'}  ${fg} (${token(fg)}) on ${bg} (${token(bg)}): ` +
      `${value.toFixed(2)}:1, minimum ${min}:1`,
  );
}
process.exit(failed ? 1 : 0);
