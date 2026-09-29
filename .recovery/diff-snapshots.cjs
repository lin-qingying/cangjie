// 从历史 git diff 的旧 blob 与完整 hunks 重建快照，并校验 diff 中的新 blob SHA。
const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process'),crypto=require('node:crypto');
const {sources}=require('./catalog.cjs');
const root=path.resolve(__dirname,'..');
function decode(text) {
  const lines=text.replace(/\r\n/g,'\n').split('\n'), result=[];
  for(let i=0;i<lines.length;i++) {
    const header=/^diff --git a\/(.+) b\/(.+)$/.exec(lines[i]);if(!header)continue;
    const file=header[2];let id=null,hunks=[];
    for(i++;i<lines.length&&!lines[i].startsWith('diff --git ');i++) {
      const index=/^index ([0-9a-f]+)\.\.([0-9a-f]+)/.exec(lines[i]);if(index){id=index;continue;}
      const h=/^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@/.exec(lines[i]);if(!h)continue;
      let oldCount=h[2]===undefined?1:+h[2],newCount=h[4]===undefined?1:+h[4];
      const before=[],after=[];let valid=true;
      while(oldCount>0||newCount>0) {
        const line=lines[++i];
        if(line===undefined){valid=false;break;}
        if(line[0]===' '){before.push(line.slice(1));after.push(line.slice(1));oldCount--;newCount--;}
        else if(line[0]==='-'){before.push(line.slice(1));oldCount--;}
        else if(line[0]==='+'){after.push(line.slice(1));newCount--;}
        else if(line.startsWith('\\ No newline'))continue;
        else{valid=false;break;}
      }
      if(!valid||oldCount!==0||newCount!==0){hunks=[];break;}
      hunks.push({start:+h[1]-(before.length?1:0),before,after});
    }
    i--;
    if(!id||!hunks.length)continue;
    const base=cp.spawnSync('git',['cat-file','blob',id[1]],{cwd:root,encoding:'utf8',maxBuffer:20000000});
    if(base.status!==0)continue;
    let content=base.stdout.split('\n'),shift=0,valid=true;
    for(const h of hunks){const at=h.start+shift;if(h.before.some((l,n)=>content[at+n]!==l)){valid=false;break;}content.splice(at,h.before.length,...h.after);shift+=h.after.length-h.before.length;}
    if(!valid)continue;
    const final=content.join('\n');
    const sha=crypto.createHash('sha1').update('blob '+Buffer.byteLength(final)+'\0').update(final).digest('hex');
    if(!sha.startsWith(id[2]))continue;
    result.push({file,content:final,oldBlob:id[1],blob:sha});
  }
  return result;
}
function snapshots() {
  const result=[];
  for(const [session,file]of sources) {
    const lines=fs.readFileSync('C:/Users/lin17/.codex/sessions/2026/09/'+file,'utf8').split(/\r?\n/);
    for(const [index,line]of lines.entries()) {
      if(!line.includes('diff --git'))continue;
      let e;try{e=JSON.parse(line);}catch{continue;}
      const it=e.payload?.item;
      if(e.timestamp>='2026-09-29T00:54:10Z'||it?.type!=='CommandExecution'||it.status!=='completed'||!it.command?.at(-1).includes('git diff'))continue;
      for(const snapshot of decode(it.stdout||''))result.push({session,line:index+1,time:e.timestamp,snapshot});
    }
  }
  return result;
}
module.exports={snapshots,decode};
if(require.main===module)process.stdout.write(JSON.stringify(snapshots().map(({snapshot,...meta})=>({...meta,file:snapshot.file,blob:snapshot.blob,bytes:snapshot.content.length})),null,2));
