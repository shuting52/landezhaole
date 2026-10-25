export default {
  async fetch(req) {
    const cors = { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'GET,OPTIONS', 'Access-Control-Allow-Headers': '*' };
    if (req.method === 'OPTIONS') return new Response(null, { headers: cors });
    const url = new URL(req.url);
    const q = (url.searchParams.get('q') || '').trim();
    if (!q) return new Response(JSON.stringify({ error: 'missing q' }), { status: 400, headers: { 'Content-Type': 'application/json', ...cors } });
    try {
      const r = await fetch('https://appteka.store/api/1/app/search?query=' + encodeURIComponent(q) + '&locale=en', { headers: { 'User-Agent': 'landezhaole-app-proxy/1.0' } });
      if (!r.ok) return new Response(JSON.stringify({ error: 'upstream ' + r.status }), { status: 502, headers: { 'Content-Type': 'application/json', ...cors } });
      const j = await r.json();
      const entries = (j.result && j.result.entries) || [];
      entries.sort((a, b) => (b.downloads || 0) - (a.downloads || 0));
      const out = entries.slice(0, 12).map(a => ({ app_id: a.app_id, label: a.label, package: a.package, ver_name: a.ver_name, size: a.size, downloads: a.downloads, icon: a.icon, category: (a.category && a.category.name && (a.category.name.zh || a.category.name.en)) || '' }));
      return new Response(JSON.stringify({ total: j.result ? j.result.total : entries.length, entries: out }), { headers: { 'Content-Type': 'application/json', 'Cache-Control': 'public, max-age=300', ...cors } });
    } catch (e) {
      return new Response(JSON.stringify({ error: String(e) }), { status: 502, headers: { 'Content-Type': 'application/json', ...cors } });
    }
  }
};
