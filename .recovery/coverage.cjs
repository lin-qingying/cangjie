// 将 reset 前的工作区清单与恢复结果、检查点及其他历史会话进行覆盖比对。
const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process');
const {eventAt}=require('./snapshots.cjs');
const root=path.resolve(__dirname,'..');
const verify=JSON.parse(fs.readFileSync(path.join(__dirname,'verified-files.json'),'utf8'));
const covered=new Set(verify.files.map(x=>x.file));
const checkpoint=cp.execFileSync('git',['diff','--name-only','737514ba7~3','737514ba7'],{cwd:root,encoding:'utf8'}).trim().split('\n');
checkpoint.forEach(x=>covered.add(x));
const original=eventAt('A-main',18).payload.item.stdout.split(/\r?\n/).flatMap(x=>{
  const m=/^([ MADRCU?!]{2}) (.+)$/.exec(x);return m?[{status:m[1],file:m[2]}]:[];
});
const missing=original.filter(x=>!covered.has(x.file));
const wanted=new Map(missing.map(x=>[x.file,{...x,sources:[]} ]));
const sessions='C:/Users/lin17/.codex/sessions/2026/09';
function* files(dir){for(const e of fs.readdirSync(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isDirectory())yield*files(p);else if(e.name.endsWith('.jsonl'))yield p;}}
(async()=>{
for(const file of files(sessions)) {
  let line=0;
  for await(const raw of require('node:readline').createInterface({input:fs.createReadStream(file,{encoding:'utf8'}),crlfDelay:Infinity})) {
    line++;if(!raw.includes('FileChange'))continue;
    let e;try{e=JSON.parse(raw);}catch{continue;}
    const item=e.payload?.item;
    if(e.timestamp>='2026-09-29T00:54:10Z'||item?.type!=='FileChange'||e.payload.type!=='item_completed'||item.status!=='completed')continue;
    for(const [p,c]of Object.entries(item.changes||{})) {
      const normalized=p.replaceAll('\\','/').replace(/^D:\/code\/intellij\/cangjie\//i,'');
      const record=wanted.get(normalized);if(record)record.sources.push({log:file,line,time:e.timestamp,type:c.type});
    }
  }
}
for(const record of wanted.values())record.sources.sort((a,b)=>a.time.localeCompare(b.time));
process.stdout.write(JSON.stringify({original:original.length,covered:original.filter(x=>covered.has(x.file)).length,checkpoint,missing:[...wanted.values()]},null,2));
})();
