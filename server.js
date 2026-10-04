const http = require('http');
const fs = require('fs');
const path = require('path');
const apiHandler = require('./api/index.js');

const PORT = process.env.PORT || 3000;
const WEB_DIR = path.join(__dirname, 'web');

const MIME_TYPES = {
    '.html': 'text/html; charset=UTF-8',
    '.css': 'text/css; charset=UTF-8',
    '.js': 'application/javascript; charset=UTF-8',
    '.json': 'application/json; charset=UTF-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon'
};

const server = http.createServer(async (req, res) => {
    const parsedUrl = new URL(req.url, `http://localhost:${PORT}`);
    const pathname = parsedUrl.pathname;

    // API Routes
    if (pathname.startsWith('/api')) {
        return apiHandler(req, res);
    }

    // Static Files
    let filePath = pathname === '/' ? '/index.html' : pathname;
    let absolutePath = path.join(__dirname, filePath);
    if (!fs.existsSync(absolutePath) || fs.statSync(absolutePath).isDirectory()) {
        absolutePath = path.join(__dirname, 'web', filePath);
    }

    fs.stat(absolutePath, (err, stats) => {
        if (err || !stats.isFile()) {
            // Fallback: try checking if it's in web without /web prefix or 404
            res.statusCode = 404;
            res.setHeader('Content-Type', 'text/plain');
            return res.end('404 Not Found');
        }

        const ext = path.extname(absolutePath).toLowerCase();
        const contentType = MIME_TYPES[ext] || 'application/octet-stream';
        res.setHeader('Content-Type', contentType);

        fs.createReadStream(absolutePath).pipe(res);
    });
});

server.listen(PORT, () => {
    console.log(`===============================================================`);
    console.log(`  QR CODE ATTENDANCE SYSTEM - LOCAL TEST SERVER (VERCEL READY) `);
    console.log(`===============================================================`);
    console.log(`>> Running locally at: http://localhost:${PORT}`);
    console.log(`>> Frontend Web UI:    http://localhost:${PORT}/index.html`);
    console.log(`>> APIs responding at: http://localhost:${PORT}/api/...`);
    console.log(`---------------------------------------------------------------`);
    console.log(`>> DEMO LOGINS:`);
    console.log(`   Admin:   admin@attendance.edu  |  admin123`);
    console.log(`   Teacher: turing@attendance.edu |  teacher123`);
    console.log(`   Student: alex@attendance.edu   |  student123`);
    console.log(`===============================================================`);
});
