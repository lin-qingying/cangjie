// 只读解码 IntelliJ LocalHistory 的目标 ContentChange 和 VFS 内容记录。
// 格式依据上游 DataInputOutputUtil、ContentChange、AppendOnlyLogOverMMappedFile、VFSContentStorageOverMMappedFile。
// 仅 open('r')；不启动 IDE、不打开其可写存储实现，每份正文必须通过 VFS SHA-1 校验。
const fs=require('node:fs'),crypto=require('node:crypto');
const base='C:/Users/lin17/AppData/Local/JetBrains/IntelliJIdea2026.2';
const history=fs.readFileSync(base+'/LocalHistory/changes.storageData');
const fd=fs.openSync(base+'/caches/content.dat','r');
function varint(b,p){let first=b[p++],value=BigInt(first);if(first>=192){value=BigInt(first-192);for(let shift=6;;shift+=7){const n=b[p++];if(n===undefined||shift>63)throw Error('Invalid varint');value|=BigInt(n&127)<<BigInt(shift);if(!(n&128))break;}}return{value:Number(value),p};}
function time(b,p){if(b[p]===255)return{value:Number(b.readBigInt64BE(p+1)),p:p+9};let n=0;for(let i=0;i<5;i++)n=n*256+b[p+i];return{value:n+33*365*24*3600*1000,p:p+5};}
function lz4(b,length){const out=Buffer.alloc(length);let i=0,o=0;while(i<b.length){const token=b[i++];let literals=token>>4;if(literals===15){let n;do{n=b[i++];literals+=n;}while(n===255);}if(i+literals>b.length||o+literals>length)throw Error('Invalid LZ4 literals');b.copy(out,o,i,i+literals);i+=literals;o+=literals;if(i===b.length)break;const offset=b.readUInt16LE(i);i+=2;let n=token&15;if(n===15){let ext;do{ext=b[i++];n+=ext;}while(ext===255);}n+=4;if(offset<=0||offset>o||o+n>length)throw Error('Invalid LZ4 match');for(let k=0;k<n;k++){out[o]=out[o-offset];o++;}}if(o!==length)throw Error('Invalid LZ4 output length');return out;}
function content(id){const at=(id-1)*4+64,h=Buffer.alloc(28);fs.readSync(fd,h,0,h.length,at);const header=h.readUInt32LE(0),length=(header&0x3fffffff)-4;if(header>>>30!==1||length<24||length>64000000)throw Error('Invalid VFS record '+id);const payload=Buffer.alloc(length);fs.readSync(fd,payload,0,length,at+4);const size=payload.readInt32LE(20);const value=size<0?lz4(payload.subarray(24),-size):payload.subarray(24);if(value.length!==Math.abs(size))throw Error('Invalid size');const hash=crypto.createHash('sha1').update(value.length+'\0').update(value).digest();if(!hash.equals(payload.subarray(0,20)))throw Error('VFS content hash mismatch');return value;}
const targets=['.idea/compiler.xml','.idea/dictionaries/project.xml','.idea/kotlinc.xml','.idea/runConfigurations/Run_All_Test.xml','analysis/stubs/test/org/cangnova/cangjie/analysis/stubs/CjoStubAstConsistencyDiagnosticTest.kt'];
function recover(selectedTargets=targets){const results=[];for(const file of selectedTargets){const full='D:/code/intellij/cangjie/'+file,needle=Buffer.from(full);for(let p=history.indexOf(needle);p>=0;p=history.indexOf(needle,p+1)){
  if(history[p-1]!==needle.length)continue;
  for(let start=Math.max(0,p-14);start<p-1;start++)try{
    const type=varint(history,start);if(type.value!==3&&type.value!==7)continue;
    const id=varint(history,type.p);if(id.p!==p-1)continue;
    let oldContent,stamp;
    if(type.value===3){oldContent=varint(history,p+needle.length);stamp=time(history,oldContent.p);}
    else {
      const entry=varint(history,p+needle.length);if(entry.value!==0)continue;
      const nameSize=history[entry.p];if(nameSize===255)continue;
      const name=history.subarray(entry.p+1,entry.p+1+nameSize).toString('latin1');
      let cursor=entry.p+1+nameSize;
      if(name==='<FILE_ID_AND_HASH>')cursor+=8;
      else if(name!==file.split('/').at(-1))continue;
      stamp={value:Number(history.readBigInt64BE(cursor))};cursor+=9;
      oldContent=varint(history,cursor);
    }
    if(stamp.value<Date.parse('2026-09-01')||stamp.value>Date.now()+86400000)continue;
    const value=content(oldContent.value);
    results.push({file,historyOffset:start,changeType:type.value,changeId:id.value,contentId:oldContent.value,oldTimestamp:new Date(stamp.value).toISOString(),bytes:value.length,sha256:crypto.createHash('sha256').update(value).digest('hex'),content:value.toString('utf8')});
  }catch{}
}}
return results;}
module.exports={recover,content,varint,time};
if(require.main===module){const results=recover();process.stdout.write(JSON.stringify(process.argv[2]==='content'?results:results.map(({content,...meta})=>meta),null,2));fs.closeSync(fd);}
