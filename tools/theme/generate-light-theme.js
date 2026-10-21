#!/usr/bin/env node
/*
 * Sinh css/common/theme-light.css: giao dien SANG cho toan bo CineFlow tu CSS toi hien co.
 *
 * Cach chay (tai thu muc goc repo):   node tools/theme/generate-light-theme.js
 * Chay lai moi khi sua file CSS hoac mau inline trong JSP.
 *
 * Nguyen tac:
 *  - Moi quy tac co mau "toi" (nen toi, chu sang, vien trang mo...) sinh 1 quy tac tuong ung
 *    duoi html[data-theme="light"], nen chi co hieu luc khi bat giao dien sang (js/theme.js).
 *  - Mau nhan (do, tim, xanh, vang...) giu nguyen; chu trang tren nen mau nhan giu nguyen.
 *  - Mau inline trong JSP (style="color:#f8fafc") duoc ghi de bang [style*=...] !important.
 */
'use strict';
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const WEBAPP = path.join(ROOT, 'src', 'main', 'webapp');
const CSS_DIR = path.join(WEBAPP, 'css');
const OUT = path.join(CSS_DIR, 'common', 'theme-light.css');
const SCOPE = 'html[data-theme="light"]';

// ---------------------------------------------------------------- mau
function parseColor(str) {
    const s = str.trim().toLowerCase();
    let m = s.match(/^#([0-9a-f]{3,8})$/);
    if (m) {
        let h = m[1];
        if (h.length === 3 || h.length === 4) h = h.split('').map(c => c + c).join('');
        const r = parseInt(h.slice(0, 2), 16), g = parseInt(h.slice(2, 4), 16), b = parseInt(h.slice(4, 6), 16);
        const a = h.length === 8 ? parseInt(h.slice(6, 8), 16) / 255 : 1;
        return { r, g, b, a };
    }
    m = s.match(/^rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)\s*(?:,\s*([\d.]+%?)\s*)?\)$/);
    if (m) {
        let a = m[4] === undefined ? 1 : (m[4].endsWith('%') ? parseFloat(m[4]) / 100 : parseFloat(m[4]));
        return { r: +m[1], g: +m[2], b: +m[3], a };
    }
    if (s === 'white') return { r: 255, g: 255, b: 255, a: 1 };
    if (s === 'black') return { r: 0, g: 0, b: 0, a: 1 };
    return null;
}

const lum = c => (0.299 * c.r + 0.587 * c.g + 0.114 * c.b) / 255;
const chroma = c => Math.max(c.r, c.g, c.b) - Math.min(c.r, c.g, c.b);
const isNeutral = c => chroma(c) < 52;
const fmt = (c) => c.a >= 1 ? '#' + [c.r, c.g, c.b].map(v => Math.round(v).toString(16).padStart(2, '0')).join('')
                             : `rgba(${Math.round(c.r)}, ${Math.round(c.g)}, ${Math.round(c.b)}, ${+c.a.toFixed(3)})`;
const SLATE_900 = { r: 15, g: 23, b: 42 };

/** role: 'bg' | 'text' | 'border' | 'shadow'. Tra ve chuoi mau moi hoac null neu giu nguyen. */
function mapColor(c, role) {
    if (!isNeutral(c)) return null;                       // mau nhan: giu
    const L = lum(c);
    if (c.a < 1) {
        if (L > 0.85) {                                    // lop trang mo tren nen toi
            if (role === 'text') return fmt({ ...SLATE_900, a: Math.max(c.a, 0.6) });
            if (role === 'border') return fmt({ ...SLATE_900, a: Math.min(Math.max(c.a, 0.08), 0.16) });
            return fmt({ ...SLATE_900, a: Math.min(c.a * 1.1, 0.5) });
        }
        if (L < 0.2) {                                     // lop den mo
            if (role === 'shadow') return fmt({ ...SLATE_900, a: +(c.a * 0.35).toFixed(3) });
            // nen toi ban trong suot (header kinh mo...) -> nen trang ban trong suot
            if (role === 'bg') return c.a >= 0.45 ? fmt({ r: 255, g: 255, b: 255, a: Math.min(0.92, c.a) })
                                                : fmt({ r: 241, g: 245, b: 249, a: Math.min(1, c.a * 2) });
            return null;
        }
        return null;
    }
    if (role === 'text') {
        if (L > 0.9) return '#0f172a';
        if (L > 0.75) return '#1e293b';
        if (L > 0.5) return '#64748b';
        return null;                                       // chu toi: da hop nen sang
    }
    if (role === 'bg') {
        if (L < 0.045) return '#eef2f7';                   // nen trang
        if (L < 0.13) return '#ffffff';                    // the / panel
        if (L < 0.25) return '#f1f5f9';                    // hover / o nhap
        if (L < 0.5) return '#e2e8f0';
        return null;
    }
    if (role === 'border') {                            // vien: luon la xam nhat tren nen sang
        if (L < 0.5 || L > 0.85) return '#e2e8f0';
        return '#cbd5e1';
    }
    if (role === 'shadow') {
        if (L < 0.2) return fmt({ ...SLATE_900, a: 0.12 });
        return null;
    }
    return null;
}

const COLOR_RE = /#[0-9a-fA-F]{3,8}\b|rgba?\([^)]*\)|\bwhite\b|\bblack\b/g;

/** Thay tung mau trong gia tri; tra ve null neu khong doi gi. */
function mapValue(value, role) {
    let changed = false;
    const out = value.replace(COLOR_RE, (tok) => {
        const c = parseColor(tok);
        if (!c) return tok;
        const n = mapColor(c, role);
        if (n && n !== tok.toLowerCase()) { changed = true; return n; }
        return tok;
    });
    return changed ? out : null;
}

function roleOf(prop) {
    const p = prop.toLowerCase();
    if (p.startsWith('--')) {
        if (/(shadow)/.test(p)) return 'shadow';
        if (/(border|line|divider|outline)/.test(p)) return 'border';
        if (/(bg|background|surface|card|footer|input|shell|sidebar|panel|overlay|base|page)/.test(p)) return 'bg';
        if (/(text|ink|muted|color|fg|title|heading|label)/.test(p)) return 'text';
        return 'auto';
    }
    if (p === 'color' || p === 'caret-color' || p === '-webkit-text-fill-color') return 'text';
    if (p.startsWith('background')) return 'bg';
    if (p.startsWith('border') || p.startsWith('outline') || p === 'column-rule-color') return 'border';
    if (p === 'box-shadow' || p === 'text-shadow' || p === 'filter') return 'shadow';
    if (p === 'fill') return 'bg';
    if (p === 'stroke') return 'border';
    return null;
}

/** Nen cua quy tac la mau nhan (nut do, badge...) => giu chu trang. */
function hasAccentBackground(decls) {
    return decls.some(d => roleOf(d.prop) === 'bg' && (d.value.match(COLOR_RE) || [])
        .map(parseColor).some(c => c && c.a > 0.5 && !isNeutral(c)));
}

// ---------------------------------------------------------------- CSS parser (du cho CSS cua du an)
function stripComments(css) { return css.replace(/\/\*[\s\S]*?\*\//g, ''); }

/** Tach chuoi theo ky tu sep o cap ngoai cung (bo qua trong ngoac va chuoi trich dan). */
function splitTop(str, sep) {
    const parts = [];
    let depth = 0, quote = null, start = 0;
    for (let i = 0; i < str.length; i++) {
        const c = str[i];
        if (quote) { if (c === '\\') i++; else if (c === quote) quote = null; continue; }
        if (c === '"' || c === "'") quote = c;
        else if (c === '(' || c === '[') depth++;
        else if (c === ')' || c === ']') depth--;
        else if (c === sep && depth === 0) { parts.push(str.slice(start, i)); start = i + 1; }
    }
    parts.push(str.slice(start));
    return parts;
}

function parseBlocks(css) {
    // tra ve [{type:'rule', selector, body} | {type:'at', prelude, children}]
    const items = [];
    let i = 0;
    while (i < css.length) {
        const open = css.indexOf('{', i);
        if (open < 0) break;
        if (css.slice(i, open).trim().startsWith('@')) {                // @import / @charset: bo qua ca cau lenh
            const stmt = splitTop(css.slice(i, open), ';');
            if (stmt.length > 1) { i += stmt[0].length + 1; continue; }
        }
        const head = css.slice(i, open).trim();
        let depth = 1, j = open + 1;
        while (j < css.length && depth > 0) { if (css[j] === '{') depth++; else if (css[j] === '}') depth--; j++; }
        const body = css.slice(open + 1, j - 1);
        if (head.startsWith('@media') || head.startsWith('@supports')) items.push({ type: 'at', prelude: head, children: parseBlocks(body) });
        else if (head.startsWith('@')) { /* @keyframes, @font-face: bo qua */ }
        else items.push({ type: 'rule', selector: head, body });
        i = j;
    }
    return items;
}

function parseDecls(body) {
    return splitTop(body, ';').map(s => s.trim()).filter(Boolean).map(s => {
        const k = s.indexOf(':');
        if (k < 0) return null;
        let value = s.slice(k + 1).trim(), important = false;
        if (/!important\s*$/i.test(value)) { important = true; value = value.replace(/!important\s*$/i, '').trim(); }
        return { prop: s.slice(0, k).trim(), value, important };
    }).filter(Boolean);
}

function scopeSelector(sel) {
    return splitTop(sel, ',').map(s => s.trim()).filter(Boolean).map(s => {
        if (s.includes('[data-theme')) return null;               // da viet tay cho tung giao dien: giu nguyen
        if (s === ':root' || s === 'html') return SCOPE;
        if (s.startsWith(':root')) return SCOPE + s.slice(5);
        if (s.startsWith('html')) return SCOPE + s.slice(4);
        if (s.startsWith('[data-bs-theme')) return null;          // khoi toi rieng cua Bootstrap (booking.css)
        return SCOPE + ' ' + s;
    }).filter(Boolean).join(',\n');
}

/** Khoi nam tren anh (banner, slide, lop phu, trailer): giu chu trang / lop phu toi o ca 2 giao dien. */
const OVER_IMAGE = /(hero|backdrop|overlay|banner|trailer|slide|carousel|poster-actions|poster-hover)/i;

/**
 * Khang dinh lai mau goc duoi tien to giao dien sang. Can thiet vi quy tac sinh ra co them tien to
 * nen manh hon quy tac goc: vd "html[..] h1" se de len ".hero-content h1 { color:#fff }" neu khong khang dinh lai.
 */
function keepOriginal(rule, decls) {
    const kept = decls.filter(d => roleOf(d.prop)).map(d => `    ${d.prop}: ${d.value}${d.important ? ' !important' : ''};`);
    const sel = scopeSelector(rule.selector);
    return kept.length && sel ? `${sel} {\n${kept.join('\n')}\n}` : null;
}

// Trang dang nhap / dang ky: .hero chi la cot chu canh form (khong nam tren anh) nen doi sang binh thuong
let currentFile = '';
const KEEP_ORIGINAL = s => /\b(eticket)\b/.test(s) || (OVER_IMAGE.test(s) && !currentFile.startsWith('auth/'));

/** Quy tac viet tay bo sung cho nhung cho tu sinh khong xu ly duoc. */
const EXTRA = `/* ---- bo sung viet tay ---- */
/* .hero cua trang chu (home/index.css, nen den cho banner) trung ten voi cot chu o trang dang nhap */
${SCOPE} .hero:not(#hero) {
    background: transparent;
}`;

function convertRule(rule) {
    // .hero cua trang auth: khong de anh huong banner trang chu (div#hero)
    if (currentFile.startsWith('auth/') && /\.hero\b(?!-)/.test(rule.selector)) {
        rule = { ...rule, selector: rule.selector.replace(/\.hero\b(?!-)/g, '.hero:not(#hero)') };
    }
    // Tach selector gop: ve / hoa don (von nen trang) va khoi tren anh giu nguyen mau, phan con lai doi sang
    const sels = splitTop(rule.selector, ',').map(s => s.trim()).filter(Boolean);
    const keep = sels.filter(KEEP_ORIGINAL), conv = sels.filter(s => !KEEP_ORIGINAL(s));
    if (keep.length && conv.length) {
        return [convertRule({ ...rule, selector: keep.join(', ') }), convertRule({ ...rule, selector: conv.join(', ') })]
            .filter(Boolean).join('\n\n') || null;
    }
    const decls = parseDecls(rule.body);
    if (keep.length) return keepOriginal(rule, decls);
    const accent = hasAccentBackground(decls);
    // Chep MOI khai bao mau (doi hoac giu nguyen): moi quy tac deu duoc cong cung do uu tien cua tien to,
    // nen thu tu de nhau giua cac quy tac giu dung nhu CSS goc (vd .tab.active van thang .tab).
    const out = [];
    let changed = false;
    for (const d of decls) {
        let role = roleOf(d.prop);
        if (!role) continue;
        const imp = d.important ? ' !important' : '';
        if (role === 'text' && accent) {                            // chu tren nen mau nhan: giu
            out.push(`    ${d.prop}: ${d.value}${imp};`);
            continue;
        }
        if (role === 'auto') {
            const first = (d.value.match(COLOR_RE) || []).map(parseColor).find(Boolean);
            if (!first) continue;
            // mau trong suot = lop phu nen; mau dac sang = chu, toi = nen
            role = first.a < 1 ? 'bg' : (lum(first) > 0.5 ? 'text' : 'bg');
        }
        const v = mapValue(d.value, role);
        if (v) changed = true;
        out.push(`    ${d.prop}: ${v || d.value}${imp};`);
    }
    if (!out.length) return null;
    if (!changed && !rule.selector.includes('.') && !rule.selector.includes('#')) return null; // the thuan (body, a...) khong doi: bo
    const sel = scopeSelector(rule.selector);
    return sel ? `${sel} {\n${out.join('\n')}\n}` : null;
}

function convertItems(items) {
    const res = [];
    for (const it of items) {
        if (it.type === 'rule') { const r = convertRule(it); if (r) res.push(r); }
        else {
            const inner = convertItems(it.children);
            if (inner.length) res.push(`${it.prelude} {\n${inner.join('\n\n')}\n}`);
        }
    }
    return res;
}

// ---------------------------------------------------------------- mau inline trong JSP
function walk(dir, exts, acc = []) {
    for (const f of fs.readdirSync(dir)) {
        const p = path.join(dir, f);
        if (fs.statSync(p).isDirectory()) walk(p, exts, acc);
        else if (exts.some(e => f.endsWith(e))) acc.push(p);
    }
    return acc;
}

function inlineRules() {
    const seen = new Map();
    for (const f of walk(WEBAPP, ['.jsp', '.jspf'])) {
        const src = fs.readFileSync(f, 'utf8');
        for (const m of src.matchAll(/style\s*=\s*"([^"]*)"/g)) {
            for (const part of m[1].split(';')) {
                const k = part.indexOf(':');
                if (k < 0) continue;
                const prop = part.slice(0, k).trim(), value = part.slice(k + 1).trim();
                if (!prop || /<%|\$\{/.test(value)) continue;
                const role = roleOf(prop);
                if (!role || role === 'auto') continue;
                const v = mapValue(value, role);
                if (!v) continue;
                const raw = part.trim();
                if (!seen.has(raw)) seen.set(raw, `${SCOPE} [style*="${raw.replace(/"/g, '\\"')}"] { ${prop}: ${v} !important; }`);
            }
        }
    }
    return [...seen.values()];
}

// ---------------------------------------------------------------- main
const files = walk(CSS_DIR, ['.css']).filter(f => path.resolve(f) !== path.resolve(OUT)).sort();
const parts = [];
for (const f of files) {
    currentFile = path.relative(CSS_DIR, f).replace(/\\/g, '/');
    const rules = convertItems(parseBlocks(stripComments(fs.readFileSync(f, 'utf8'))));
    if (rules.length) parts.push(`/* ---- ${path.relative(CSS_DIR, f).replace(/\\/g, '/')} ---- */\n${rules.join('\n\n')}`);
}
// Khoi <style> viet thang trong JSP (vd views/report/header.jspf)
for (const f of walk(WEBAPP, ['.jsp', '.jspf']).sort()) {
    currentFile = '';
    const src = fs.readFileSync(f, 'utf8');
    const css = [...src.matchAll(/<style[^>]*>([\s\S]*?)<\/style>/gi)].map(m => m[1]).join('\n');
    if (!css.trim() || /<%|\$\{/.test(css)) continue;
    const rules = convertItems(parseBlocks(stripComments(css)));
    if (rules.length) parts.push(`/* ---- <style> trong ${path.relative(WEBAPP, f).replace(/\\/g, '/')} ---- */\n${rules.join('\n\n')}`);
}
const inline = inlineRules();

const header = `/* =========================================================================
   GIAO DIEN SANG (tu sinh) - KHONG SUA TAY.
   Sinh bang: node tools/theme/generate-light-theme.js
   Chi co hieu luc khi <html data-theme="light"> (js/theme.js dat theo lua chon nguoi dung).
   ========================================================================= */
${SCOPE} { color-scheme: light; }
`;
fs.writeFileSync(OUT, header + '\n' + parts.join('\n\n') + '\n\n/* ---- mau inline trong JSP ---- */\n' + inline.join('\n') + '\n\n' + EXTRA + '\n');
console.log(`theme-light.css: ${files.length} file CSS, ${parts.reduce((n, p) => n + (p.match(/\{/g) || []).length, 0)} quy tac, ${inline.length} mau inline`);
