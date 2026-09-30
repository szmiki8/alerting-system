// Dev-server proxy (FE-03, ADR-02, architecture Section 11 "Local development").
// The browser only talks to the dev server, so it sees one origin as in production behind Nginx.
// Paths that belong to the Core are forwarded; everything else is served by `ng serve`.
// Override the Core location without editing this file: CORE_URL=http://localhost:9090 npm start
const coreUrl = process.env['CORE_URL'] ?? 'http://localhost:8080';

const toCore = {
  target: coreUrl,
  secure: false,
  // Keep the browser's Host header (no changeOrigin) and add X-Forwarded-Host/Proto/Port/For,
  // so the Core builds the Google OAuth redirect URI for the dev-server origin (OP-02).
  changeOrigin: false,
  xfwd: true,
  // Safety net: rewrite redirects that point at the Core's own host back to the dev-server origin.
  // Redirects to other hosts (for example Google) are left alone.
  autoRewrite: true,
};

export default {
  '/api/**': toCore,
  '/oauth2/**': toCore,
  '/login/**': toCore,
  '/logout': toCore,
};
