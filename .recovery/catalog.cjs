// 只读提取会话补丁；历史 shell/工具调用永不执行。
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const base = 'C:/Users/lin17/.codex/sessions/2026/09';
const sources = [
  ['B-main','28/rollout-2026-09-28T15-41-00-01a0e6f5-c134-79c0-bcb8-6fc99b3adab5.jsonl'],
  ['B-sub1','28/rollout-2026-09-28T15-42-05-01a0e6f6-bc5a-7020-8a2a-51ce4e495029.jsonl'],
  ['B-sub2','28/rollout-2026-09-28T15-42-17-01a0e6f6-ee4a-7d20-97b6-41aa36424a83.jsonl'],
  ['B-sub3','28/rollout-2026-09-28T15-42-26-01a0e6f7-105c-7922-b5ff-47ff21da81e3.jsonl'],
  ['A-main','29/rollout-2026-09-29T07-07-40-01a0ea46-2571-7a51-ab29-168d65f086a0.jsonl'],
  ['A-sub1','29/rollout-2026-09-29T07-08-55-01a0ea47-4725-75d0-a197-d714bd4edfa0.jsonl'],
  ['A-sub2','29/rollout-2026-09-29T07-09-03-01a0ea47-677d-7192-9a2c-42e0ad39f53d.jsonl'],
  ['A-sub3','29/rollout-2026-09-29T07-09-21-01a0ea47-ad30-7192-80bc-4bf5479628d2.jsonl'],
];
function textOf(output) {
  if (typeof output === 'string') return output;
  return (output || []).map(x => x.text || '').join('\n');
}
function literalAt(code, start) {
  while (/\s/.test(code[start] || 'x')) start++;
  const quote=code[start];
  if (!['"',"'",'`'].includes(quote)) return null;
  let end=start+1, dynamic=false;
  for (;end<code.length;end++) {
    if (code[end]==='\\') {end++;continue;}
    if (quote==='`' && code.slice(end,end+2)==='${') dynamic=true;
    if (code[end]===quote) break;
  }
  if (end===code.length || dynamic) return null;
  const raw=code.slice(start,end+1);
  let value;
  try { value=vm.runInNewContext(raw,Object.create(null),{timeout:100,contextCodeGeneration:{strings:false,wasm:false}}); }
  catch { return null; }
  return typeof value==='string' ? {value,end:end+1} : null;
}
function catalog() {
  const calls=[];
  for (const [session,relative] of sources) {
    const file=path.join(base,relative);
    const events=fs.readFileSync(file,'utf8').split(/\r?\n/).map((line,index)=>{
      try { return {...JSON.parse(line),line:index+1}; } catch { return null; }
    }).filter(Boolean);
    const outputs=new Map(events.filter(e=>['custom_tool_call_output','function_call_output'].includes(e.payload?.type)).map(e=>[e.payload.call_id,textOf(e.payload.output)]));
    for (const e of events) {
      if (e.timestamp >= '2026-09-29T00:54:10.000Z') continue;
      const p=e.payload;
      if (e.type!=='response_item' || !['custom_tool_call','function_call'].includes(p?.type)) continue;
      const code=p.input ?? p.arguments ?? '';
      if (!code.includes('apply_patch') && p.name!=='apply_patch') continue;
      const result=outputs.get(p.call_id) || '';
      const patches=[], dynamic=[];
      if (p.name==='apply_patch') patches.push(code);
      else {
        const pattern=/(?:tools\.)?apply_patch\s*\(/g;
        for (let m;(m=pattern.exec(code));) {
          const literal=literalAt(code,pattern.lastIndex);
          if (literal && /^\s*\)/.test(code.slice(literal.end))) patches.push(literal.value);
          else dynamic.push(m.index);
        }
      }
      calls.push({session,file,line:e.line,time:e.timestamp,id:p.call_id,code,result,patches,dynamic,failed:/Script failed|Script error:|verification failed/.test(result)});
    }
  }
  return calls.sort((a,b)=>a.time.localeCompare(b.time)||a.session.localeCompare(b.session)||a.line-b.line);
}
module.exports={catalog,literalAt,textOf,sources};
if (require.main===module) {
  const calls=catalog();
  if (process.argv[2]==='call') process.stdout.write(JSON.stringify(calls[Number(process.argv[3])]));
  else if (process.argv[2]==='all') process.stdout.write(JSON.stringify(calls));
  else process.stdout.write(JSON.stringify({
    calls:calls.length,literalPatches:calls.reduce((n,c)=>n+c.patches.length,0),
    failed:calls.filter(c=>c.failed).map((c,i)=>({session:c.session,line:c.line,literals:c.patches.length,dynamic:c.dynamic.length,error:c.result.slice(0,250)})),
    dynamic:calls.map((c,index)=>({...c,index})).filter(c=>c.dynamic.length).map(c=>({index:c.index,session:c.session,line:c.line,time:c.time,code:c.code.slice(0,250)})),
  },null,2));
}
