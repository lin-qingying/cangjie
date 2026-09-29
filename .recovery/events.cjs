// 使用工具记录的成功 FileChange 事件恢复；动态表达式与失败调用无需猜测。
const fs=require('node:fs');
const {sources}=require('./catalog.cjs');
function events() {
  const result=[];
  for(const [session,file]of sources) {
    const lines=fs.readFileSync('C:/Users/lin17/.codex/sessions/2026/09/'+file,'utf8').split(/\r?\n/);
    for(const [index,line]of lines.entries()) {
      let e;try{e=JSON.parse(line);}catch{continue;}
      const item=e.payload?.item;
      if(e.timestamp>='2026-09-29T00:54:10.000Z'||e.payload?.type!=='item_completed'||item?.type!=='FileChange'||item.status!=='completed')continue;
      result.push({session,line:index+1,time:e.timestamp,changes:item.changes});
    }
  }
  return result.sort((a,b)=>a.time.localeCompare(b.time)||a.session.localeCompare(b.session)||a.line-b.line);
}
function patch(file,change) {
  let body;
  if(change.type==='add')body='*** Add File: '+file+'\n'+change.content.replace(/\r\n/g,'\n').replace(/\n$/,'').split('\n').map(x=>'+'+x).join('\n');
  else if(change.type==='delete')body='*** Delete File: '+file;
  else if(change.type==='update') {
    body='*** Update File: '+file+'\n'+(change.move_path?'*** Move to: '+change.move_path+'\n':'')+change.unified_diff.replace(/^@@[^\n]*$/gm,'@@').trimEnd();
  } else throw Error('Unknown FileChange '+change.type);
  return '*** Begin Patch\n'+body+'\n*** End Patch';
}
module.exports={events,patch};
