// @ts-check
const eslint = require('@eslint/js');
const { defineConfig, globalIgnores } = require('eslint/config');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
const prettier = require('eslint-config-prettier/flat');

function browserStorageRestrictions() {
  return ['localStorage', 'sessionStorage', 'indexedDB'].map((name) => ({
    name,
    message: 'No tokens, session or user data in browser storage (architecture Section 11).',
  }));
}

module.exports = defineConfig([
  globalIgnores([
    'dist/',
    'coverage/',
    '.angular/',
    'out-tsc/',
    'test-results/',
    'playwright-report/',
  ]),
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'app',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'app',
          style: 'kebab-case',
        },
      ],
      // Zoneless app driven by signals (ADR-12): every component uses OnPush.
      '@angular-eslint/prefer-on-push-component-change-detection': 'error',
      // No tokens, session or user data in browser storage (architecture Section 11, FE-07).
      'no-restricted-globals': ['error', ...browserStorageRestrictions()],
      'no-restricted-properties': [
        'error',
        ...['window', 'globalThis', 'self'].flatMap((object) =>
          browserStorageRestrictions().map(({ name, message }) => ({
            object,
            property: name,
            message,
          })),
        ),
      ],
    },
  },
  {
    // Tests may read and clear browser storage to prove that the application leaves it empty.
    files: ['**/*.spec.ts'],
    rules: { 'no-restricted-globals': 'off', 'no-restricted-properties': 'off' },
  },
  {
    files: ['**/*.html'],
    // templateAccessibility: alt text, label association, valid ARIA, keyboard events (NFR-13).
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {},
  },
  // Must stay last: turns off every rule that would conflict with Prettier formatting.
  prettier,
]);
