// Page d'accueil du portail-foyer (lien "<- Portail" de l'en-tete). Meme logique que
// l'URL de l'API : HTTPS via Tailscale si la page est en HTTPS, sinon HTTP en LAN.
const PORT_PORTAIL_HTTP = 8185
const PORT_PORTAIL_HTTPS = 8285

export const PORTAL_URL = window.location.protocol === 'https:'
  ? `https://${window.location.hostname}:${PORT_PORTAIL_HTTPS}`
  : `http://${window.location.hostname}:${PORT_PORTAIL_HTTP}`
