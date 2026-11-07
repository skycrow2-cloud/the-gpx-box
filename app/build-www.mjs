// Copie la page du site (../index.html + ../aide) dans www/ pour l'application Android,
// en y ajoutant le runtime Capacitor (accès au GPS en arrière-plan, partage de fichiers).
import { cpSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';

rmSync('www', { recursive: true, force: true });
mkdirSync('www', { recursive: true });
let html = readFileSync('../index.html', 'utf8');
if (!html.includes('</head>')) throw new Error('index.html : </head> introuvable');
html = html.replace('</head>', '<script src="capacitor.js"></script>\n</head>');
writeFileSync('www/index.html', html);
cpSync('node_modules/@capacitor/core/dist/capacitor.js', 'www/capacitor.js');
cpSync('../aide', 'www/aide', { recursive: true });
console.log('www/ prêt');
