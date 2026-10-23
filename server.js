const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = process.env.PORT || 10000;

// Load initial articles
let articles = [];
try {
  const data = fs.readFileSync(path.join(__dirname, 'articles.json'), 'utf8');
  articles = JSON.parse(data);
} catch (e) {
  console.error("Notice: articles.json not found or failed to parse:", e.message);
}

let bookmarks = new Set();
let readingHistory = [];

const categories = [
  { name: "HOME", displayName: "Home", description: "Top stories & curated editorial selection", color: "#1e293b" },
  { name: "LATEST", displayName: "Latest News", description: "Chronological updates across all desks", color: "#0f766e" },
  { name: "WORLD", displayName: "World", description: "International affairs and developments", color: "#2563eb" },
  { name: "INDIA", displayName: "India", description: "National governance, industry, and culture", color: "#d97706" },
  { name: "TECHNOLOGY", displayName: "Technology", description: "Artificial intelligence, software, and hardware", color: "#6366f1" },
  { name: "BUSINESS", displayName: "Business", description: "Global markets, finance, and economics", color: "#10b981" },
  { name: "SCIENCE", displayName: "Science", description: "Space exploration, genetics, and climate", color: "#0ea5e9" },
  { name: "SPORTS", displayName: "Sports", description: "Championships, athletics, and global games", color: "#ea580c" },
  { name: "ENTERTAINMENT", displayName: "Entertainment", description: "Cinema, arts, literature, and culture", color: "#a855f7" }
];

function sendJson(res, statusCode, data) {
  const json = JSON.stringify(data);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=UTF-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization'
  });
  res.end(json);
}

const server = http.createServer((req, res) => {
  const reqUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = reqUrl.pathname;
  const method = req.method.toUpperCase();
  const getQuery = (key) => reqUrl.searchParams.get(key) || '';

  // CORS preflight
  if (method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization'
    });
    res.end();
    return;
  }

  // Health check for Render
  if (pathname === '/api/health') {
    return sendJson(res, 200, { status: "UP", name: "NEWSROOM Editorial", version: "2.0.0" });
  }

  // Categories
  if (pathname === '/api/categories') {
    return sendJson(res, 200, categories);
  }

  // Articles
  if (pathname === '/api/articles') {
    const type = getQuery('type').toLowerCase();
    const cat = (getQuery('category') || 'HOME').toUpperCase();

    let list = articles.map(a => ({
      ...a,
      isBookmarked: bookmarks.has(a.id)
    }));

    if (type === 'featured') {
      list = list.filter(a => a.isFeatured);
    } else if (type === 'trending') {
      list = list.filter(a => a.isTrending);
    } else if (cat && cat !== 'HOME' && cat !== 'LATEST') {
      list = list.filter(a => a.category.toUpperCase() === cat);
    }

    return sendJson(res, 200, list);
  }

  // Search
  if (pathname === '/api/search') {
    const q = getQuery('q').trim().toLowerCase();
    if (!q) return sendJson(res, 200, []);
    const results = articles.filter(a => 
      a.title.toLowerCase().includes(q) || 
      a.summary.toLowerCase().includes(q) || 
      a.author.toLowerCase().includes(q) ||
      a.content.toLowerCase().includes(q)
    ).map(a => ({ ...a, isBookmarked: bookmarks.has(a.id) }));
    return sendJson(res, 200, results);
  }

  // Refresh
  if (pathname === '/api/refresh') {
    return sendJson(res, 200, { status: "success", message: "Newsroom archive refreshed successfully" });
  }

  // Bookmarks
  if (pathname.startsWith('/api/bookmarks')) {
    if (method === 'GET') {
      const bookmarked = articles.filter(a => bookmarks.has(a.id)).map(a => ({ ...a, isBookmarked: true }));
      return sendJson(res, 200, bookmarked);
    }

    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      let articleId = null;
      try {
        const parsed = JSON.parse(body || '{}');
        articleId = parsed.articleId;
      } catch (e) {}

      if (!articleId && pathname.split('/')[3]) {
        articleId = pathname.split('/')[3];
      }

      if (method === 'POST') {
        if (articleId) bookmarks.add(articleId);
        return sendJson(res, 200, { status: "success", bookmarked: true });
      }

      if (method === 'DELETE') {
        if (articleId) bookmarks.delete(articleId);
        return sendJson(res, 200, { status: "success", bookmarked: false });
      }

      res.writeHead(405);
      res.end('Method Not Allowed');
    });
    return;
  }

  // History
  if (pathname === '/api/history') {
    if (method === 'GET') {
      return sendJson(res, 200, readingHistory);
    }
    if (method === 'POST') {
      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const parsed = JSON.parse(body || '{}');
          const art = articles.find(a => a.id === parsed.articleId);
          if (art) {
            readingHistory = readingHistory.filter(h => h.article.id !== art.id);
            readingHistory.unshift({
              article: { ...art, isBookmarked: bookmarks.has(art.id) },
              readAt: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
            });
            if (readingHistory.length > 50) readingHistory.pop();
          }
        } catch (e) {}
        return sendJson(res, 200, { status: "success" });
      });
      return;
    }
    if (method === 'DELETE') {
      readingHistory = [];
      return sendJson(res, 200, { status: "cleared" });
    }
  }

  // Web UI (HTML)
  const indexPath = path.join(__dirname, 'src', 'main', 'resources', 'web', 'index.html');
  fs.readFile(indexPath, (err, content) => {
    if (err) {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=UTF-8' });
      res.end('<h1>NEWSROOM Cloud Web Service</h1><p>Running successfully.</p>');
    } else {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=UTF-8' });
      res.end(content);
    }
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`=================================================`);
  console.log(` NEWSROOM Web Service Active!`);
  console.log(` Listening on port: ${PORT}`);
  console.log(`=================================================`);
});
