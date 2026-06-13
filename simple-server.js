const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 10086;
const ROOT = __dirname;

const mimeTypes = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.wav': 'audio/wav',
  '.mp4': 'video/mp4',
  '.woff': 'application/font-woff',
  '.ttf': 'application/font-ttf',
  '.eot': 'application/vnd.ms-fontobject',
  '.otf': 'application/font-otf',
  '.wasm': 'application/wasm'
};

const server = http.createServer((req, res) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.url}`);

  let filePath = path.join(ROOT, req.url === '/' ? 'index.html' : req.url);
  
  if (req.url.startsWith('/lib/')) {
    filePath = path.join(ROOT, req.url);
  } else if (req.url.startsWith('/src/')) {
    filePath = path.join(ROOT, req.url);
  } else if (req.url === '/' || req.url === '/index.html') {
    filePath = path.join(ROOT, 'src', 'index.html');
  }

  const extname = String(path.extname(filePath)).toLowerCase();
  const contentType = mimeTypes[extname] || 'application/octet-stream';

  fs.readFile(filePath, (error, content) => {
    if (error) {
      if (error.code === 'ENOENT') {
        res.writeHead(404, { 'Content-Type': 'text/html' });
        res.end('<h1>404 Not Found</h1>', 'utf-8');
      } else {
        res.writeHead(500);
        res.end(`Server Error: ${error.code}`, 'utf-8');
      }
    } else {
      res.writeHead(200, { 'Content-Type': contentType });
      res.end(content, 'utf-8');
    }
  });
});

server.listen(PORT, () => {
  console.log(`\n========================================`);
  console.log(`  移动端离线上报与同步 - 预览服务`);
  console.log(`  服务已启动: http://localhost:${PORT}`);
  console.log(`========================================\n`);
});
