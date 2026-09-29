// 只读审计历史 FileChange；不执行历史命令，不向原工作区写文件。
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const cp = require('node:child_process');
const {events} = require('./events.cjs');
const {sources} = require('./catalog.cjs');
const originalRoot = 'D:/code/intellij/cangjie/intellij-ide';
const normalize = p => p.replace(/\\+/g, '/');
const relative = p => {
  const normalized = normalize(p);
  const prefix = 'D:/code/intellij/cangjie/intellij-ide/';
  if (!normalized.startsWith(prefix)) throw Error('Unexpected path: ' + p);
  const result = normalized.slice(prefix.length);
  if (result.split('/').includes('..')) throw Error('Parent traversal: ' + p);
  return result;
};
const textLines = text => text.replace(/\r\n/g, '\n').replace(/\n$/, '').split('\n');
const hash = text => crypto.createHash('sha256').update(text).digest('hex');
const histories = new Map();
let eventCount = 0;
for (const event of events().filter(e => e.session.startsWith('B'))) {
  for (const [absolute, change] of Object.entries(event.changes)) {
    if (!normalize(absolute).startsWith(originalRoot + '/')) continue;
    const oldPath = relative(absolute);
    const finalPath = change.move_path ? relative(change.move_path) : oldPath;
    const history = histories.get(oldPath) || [];
    history.push({session: event.session, line: event.line, time: event.time, oldPath, finalPath, change});
    if (change.move_path) histories.delete(oldPath);
    histories.set(finalPath, history);
    eventCount++;
  }
}
function hunks(diff) {
  const parsed = [];
  for (const line of diff.replace(/\r\n/g, '\n').split('\n')) {
    const header = /^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@/.exec(line);
    if (header) { parsed.push({newStart: Number(header[3]), before: [], after: []}); continue; }
    if (!parsed.length || line.startsWith('\\') || line === '') continue;
    const h = parsed.at(-1), value = line.slice(1);
    if (line[0] === ' ' || line[0] === '-') h.before.push(value);
    if (line[0] === ' ' || line[0] === '+') h.after.push(value);
  }
  return parsed;
}
function reverseUpdate(lines, diff) {
  const evidence = [];
  for (const h of hunks(diff).reverse()) {
    const hits = [];
    for (let i = 0; i <= lines.length - h.after.length; i++) {
      if (h.after.every((line, j) => line === lines[i + j])) hits.push(i);
    }
    if (hits.length !== 1) return {ok: false, evidence, expectedNewLine: h.newStart, matches: hits.length, after: h.after};
    evidence.push({expectedNewLine: h.newStart, matchedCurrentLine: hits[0] + 1, afterLines: h.after.length});
    lines.splice(hits[0], h.after.length, ...h.before);
  }
  return {ok: true, evidence};
}
const reports = [];
for (const [finalPath, history] of histories) {
  const absolute = path.join(originalRoot, finalPath);
  const exists = fs.existsSync(absolute);
  const current = exists ? fs.readFileSync(absolute, 'utf8') : null;
  const currentBytes = exists ? fs.readFileSync(absolute) : null;
  const lines = current === null ? null : textLines(current);
  const checks = [];
  let ok = true;
  for (const event of [...history].reverse()) {
    const c = event.change;
    let check;
    if (c.type === 'update') check = lines === null ? {ok:false, reason:'final file absent'} : reverseUpdate(lines, c.unified_diff);
    else if (c.type === 'add') check = {ok: lines !== null && JSON.stringify(lines) === JSON.stringify(textLines(c.content)), kind:'full added content after reversing later updates'};
    else if (c.type === 'delete') check = {ok: !exists, kind:'historical deletion'};
    else check = {ok:false, reason:'unknown change type'};
    checks.push({session:event.session, line:event.line, log:sources.find(([s]) => s === event.session)[1], time:event.time, changeType:c.type, from:event.oldPath, to:event.finalPath, ...check});
    if (!check.ok) {ok=false; break;}
  }
  reports.push({path:finalPath, exists, sha256:currentBytes && hash(currentBytes), bytes:currentBytes?.length ?? 0,
    preserved:ok, historicalChanges:history.length, checkedChanges:checks.length, checks});
}
const git = args => cp.execFileSync('git', ['-C', originalRoot, ...args], {encoding:'utf8', windowsHide:true}).trimEnd();
const result = {
  auditedAtUtc:new Date().toISOString(), originalRoot,
  method:'Read-only reverse verification of every successful B-session FileChange hunk against current final paths; historical add content checked exactly after later updates reversed in memory. CRLF/LF normalized only for content comparison. No historical shell executed.',
  cutoff:'2026-09-29T00:54:10.000Z', head:git(['rev-parse','HEAD']), status:git(['status','--short']),
  historicalChanges:eventCount, finalFiles:reports.length, preservedFiles:reports.filter(r=>r.preserved).length,
  unresolvedFiles:reports.filter(r=>!r.preserved).map(r=>r.path), files:reports,
};
process.stdout.write(JSON.stringify(result, null, 2));
