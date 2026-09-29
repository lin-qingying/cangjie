// 定位未出现在 FileChange 中的文件输出或自动更新命令，绝不执行历史命令。
const fs=require('node:fs'),readline=require('node:readline');
const logs=[
'C:/Users/lin17/.codex/sessions/2026/09/25/rollout-2026-09-25T15-29-33-01a0d778-31e8-7762-bad1-57c106ddc2f9.jsonl',
'C:/Users/lin17/.codex/sessions/2026/09/28/rollout-2026-09-28T12-07-34-01a0e632-5901-7050-b92f-28301bb5767b.jsonl',
];
(async()=>{
for(const log of logs){let line=0;
for await(const raw of readline.createInterface({input:fs.createReadStream(log,{encoding:'utf8'}),crlfDelay:Infinity})) {
  line++;if(!raw.includes(process.argv[2]))continue;
  let e;try{e=JSON.parse(raw);}catch{continue;}
  if(e.timestamp>='2026-09-29T00:54:10Z')continue;
  if(e.payload?.type==='custom_tool_call'||e.payload?.type==='function_call')console.log(JSON.stringify({log,line,time:e.timestamp,code:(e.payload.input||e.payload.arguments||'').slice(0,16000)}));
  else if(e.payload?.item?.type==='CommandExecution'&&e.payload.item.status==='completed')console.log(JSON.stringify({log,line,time:e.timestamp,cmd:e.payload.item.command.at(-1),output:e.payload.item.stdout?.slice(0,2000)}));
}
}
})();
