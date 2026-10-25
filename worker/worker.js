const CORS = { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET,OPTIONS', 'Access-Control-Allow-Headers': '*' };
const PROXY = 'https://landezhaole-app-proxy.307779523.workers.dev';
async function getLink(appId, cache) {
  const key = new Request(PROXY + '/link/' + appId);
  const hit = await cache.match(key);
  if (hit) { const j = await hit.json(); if (j.link) return j.link; }
  const r = await fetch('https://appteka.store/api/1/app/info?app_id=' + encodeURIComponent(appId) + '&locale=en', { headers: { 'User-Agent': 'landezhaole-app-proxy/1.0' } });
  if (!r.ok) return null;
  const j = await r.json();
  const link = j.result && j.result.link;
  if (!link) return null;
  await cache.put(key, new Response(JSON.stringify({ link }), { headers: { 'Content-Type': 'application/json', 'Cache-Control': 'max-age=600' } }));
  return link;
}
export default {
  async fetch(req) {
    if (req.method === 'OPTIONS') return new Response(null, { headers: CORS });
    const url = new URL(req.url);
    const cache = caches.default;
    try {
      if (url.pathname === '/info') {
        const appId = url.searchParams.get('app_id') || '';
        const r = await fetch('https://appteka.store/api/1/app/info?app_id=' + encodeURIComponent(appId) + '&locale=en', { headers: { 'User-Agent': 'landezhaole-app-proxy/1.0' } });
        if (!r.ok) return new Response(JSON.stringify({ error: 'info ' + r.status }), { status: 502, headers: { 'Content-Type': 'application/json', ...CORS } });
        const j = await r.json();
        const info = j.result && j.result.info;
        if (!info) return new Response(JSON.stringify({ error: 'no info' }), { status: 502, headers: { 'Content-Type': 'application/json', ...CORS } });
        return new Response(JSON.stringify({ size: info.size, label: info.label, package: info.package, ver_name: info.ver_name }), { headers: { 'Content-Type': 'application/json', ...CORS } });
      }
      if (url.pathname === '/segment') {
        const appId = url.searchParams.get('app_id') || '';
        const start = parseInt(url.searchParams.get('start') || '0', 10);
        const end = parseInt(url.searchParams.get('end') || '0', 10);
        const link = await getLink(appId, cache);
        if (!link) return new Response(JSON.stringify({ error: 'no link' }), { status: 502, headers: { 'Content-Type': 'application/json', ...CORS } });
        const r = await fetch(link, { headers: { 'Range': 'bytes=' + start + '-' + end, 'User-Agent': 'landezhaole-app-proxy/1.0' } });
        return new Response(r.body, { status: r.status, headers: { 'Content-Type': 'application/vnd.android.package-archive', 'Content-Length': r.headers.get('Content-Length') || String(end - start + 1), 'Content-Range': r.headers.get('Content-Range') || ('bytes ' + start + '-' + end + '/*'), 'Accept-Ranges': 'bytes', ...CORS } });
      }
      // search (default)
      const q = (url.searchParams.get('q') || '').trim();
      if (!q) return new Response(JSON.stringify({ error: 'missing q' }), { status: 400, headers: { 'Content-Type': 'application/json', ...CORS } });
      const offset = parseInt(url.searchParams.get('offset') || '0', 10) || 0;
      const r = await fetch('https://appteka.store/api/1/app/search?query=' + encodeURIComponent(q) + '&locale=en&offset=' + offset, { headers: { 'User-Agent': 'landezhaole-app-proxy/1.0' } });
      if (!r.ok) return new Response(JSON.stringify({ error: 'upstream ' + r.status }), { status: 502, headers: { 'Content-Type': 'application/json', ...CORS } });
      const j = await r.json();
      const entries = (j.result && j.result.entries) || [];
      entries.sort((a, b) => (b.downloads || 0) - (a.downloads || 0));
      const out = entries.map(a => ({ app_id: a.app_id, label: a.label, package: a.package, ver_name: a.ver_name, size: a.size, downloads: a.downloads, icon: a.icon, category: (a.category && a.category.name && (a.category.name.zh || a.category.name.en)) || '' }));
      return new Response(JSON.stringify({ total: j.result ? j.result.total : entries.length, entries: out }), { headers: { 'Content-Type': 'application/json', 'Cache-Control': 'public, max-age=300', ...CORS } });
    } catch (e) {
      return new Response(JSON.stringify({ error: String(e) }), { status: 502, headers: { 'Content-Type': 'application/json', ...CORS } });
    }
  }
};
