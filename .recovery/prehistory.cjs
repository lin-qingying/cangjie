// 补回 reset 前清单中、早于 import/CJMP 两会话的未提交变更。
const fs=require('node:fs'),path=require('node:path'),readline=require('node:readline');
const {eventAt}=require('./snapshots.cjs');
async function prehistory() {
  const coverage=JSON.parse(fs.readFileSync(path.join(__dirname,'coverage-report.json'),'utf8'));
  const logs=[...new Set(coverage.missing.flatMap(x=>x.sources.map(s=>s.log)))];
  const original=eventAt('A-main',18).payload.item.stdout.split(/\r?\n/).flatMap(x=>{const m=/^([ MADRCU?!]{2}) (.+)$/.exec(x);return m?[m[2]]:[];});
  const wanted=new Set(original);
  const checkpoint=new Set(coverage.checkpoint);
  const dirs=original.filter(x=>x.endsWith('/'));
  const seeded=new Map([
    ['analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/references/CfirReferenceResolveHelper.kt','2026-09-28T07:56:00Z'],
    ['cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirImportBindingResolver.kt','2026-09-28T07:56:00Z'],
    ['psi/src/org/cangnova/cangjie/psi/psiUtil/CjPsiUtil.kt','2026-09-28T07:56:00Z'],
    ['psi/src/org/cangnova/cangjie/psi/CjImportDirectiveItem.kt','2026-09-28T08:00:15Z'],
    ['analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirImportPlanning.kt','2026-09-28T08:00:00Z'],
  ]);
  const result=[];
  for(const log of logs) {
    let line=0;
    for await(const raw of readline.createInterface({input:fs.createReadStream(log,{encoding:'utf8'}),crlfDelay:Infinity})) {
      line++;if(!raw.includes('FileChange'))continue;
      let e;try{e=JSON.parse(raw);}catch{continue;}
      const it=e.payload?.item;
      if(e.timestamp<'2026-09-27T06:41:52Z'||e.timestamp>='2026-09-29T00:54:10Z'||e.payload?.type!=='item_completed'||it?.type!=='FileChange'||it.status!=='completed')continue;
      const changes={};
      for(const [p,change]of Object.entries(it.changes)) {
        const f=p.replaceAll('\\','/').replace(/^D:\/code\/intellij\/cangjie\//i,'');
        if(!wanted.has(f)&&!dirs.some(d=>f.startsWith(d)))continue;
        if(checkpoint.has(f))continue;
        if(seeded.has(f)&&e.timestamp<seeded.get(f))continue;
        changes[p]=change;
      }
      if(Object.keys(changes).length)result.push({session:'prior:'+path.basename(log),line,time:e.timestamp,changes});
    }
  }
  return result;
}
module.exports={prehistory};
