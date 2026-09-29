// 只生成实际恢复 patch；写盘必须由调用方的 apply_patch 工具完成。
const fs=require('node:fs');
const path=require('node:path');
const cp=require('node:child_process');
const crypto=require('node:crypto');
const {replay,root}=require('./replay.cjs');
const hash=s=>s===null?null:crypto.createHash('sha256').update(s).digest('hex');
async function prepare(offset=0,maxChars=100000) {
  const r=await replay();
  if(r.issues.length)throw Error(JSON.stringify(r.issues));
  const changed=[...r.state].filter(([f,v])=>v!==r.initial.get(f));
  const patches=[];let chars=0,index=offset;
  for(;index<changed.length;index++) {
    const [file,value]=changed[index],absolute=path.join(root,file);
    let old=null;try{old=fs.readFileSync(absolute,'utf8').replace(/\r\n/g,'\n');}catch(e){if(e.code!=='ENOENT')throw e;}
    if(old===value)continue;
    let body;
    if(value===null)body='*** Delete File: '+absolute;
    else if(old===null)body='*** Add File: '+absolute+'\n'+value.replace(/\n$/,'').split('\n').map(x=>'+'+x).join('\n');
    else {
      const diff=cp.spawnSync('git',['diff','--no-index','--no-ext-diff','--no-color','--text','--ignore-space-at-eol','--unified=3','--',absolute,'-'],{cwd:root,input:value,encoding:'utf8',maxBuffer:20000000});
      if(diff.status>1)throw Error(diff.stderr);
      const start=diff.stdout.indexOf('@@ ');
      if(start<0)throw Error('Nonidentical text without diff: '+file);
      body='*** Update File: '+absolute+'\n'+diff.stdout.slice(start).replace(/^@@[^\n]*$/gm,'@@').replace(/\r\n/g,'\n').trimEnd();
    }
    const patch='*** Begin Patch\n'+body+'\n*** End Patch';
    if(chars+patch.length>maxChars&&patches.length)break;
    patches.push({file,patch,hash:hash(value)});chars+=patch.length;
  }
  return {next:index,total:changed.length,patches};
}
async function verify() {
  const r=await replay(),files=[];
  for(const [file,value]of r.state) {
    if(value===r.initial.get(file))continue;
    let actual=null;try{actual=fs.readFileSync(path.join(root,file),'utf8').replace(/\r\n/g,'\n');}catch(e){if(e.code!=='ENOENT')throw e;}
    files.push({file,expected:hash(value),actual:hash(actual),matches:value===actual,history:r.history.get(file)});
  }
  return {issues:r.issues,files};
}
module.exports={prepare,verify};
if(require.main===module)(async()=>process.stdout.write(JSON.stringify(process.argv[2]==='verify'?await verify():await prepare(+process.argv[2]||0,+process.argv[3]||100000))))();
