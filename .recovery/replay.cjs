// 在内存中重放已记录的文本补丁，只向 stdout 输出恢复计划，不写任何源码。
const fs=require('node:fs');
const path=require('node:path');
const cp=require('node:child_process');
const vm=require('node:vm');
const {catalog}=require('./catalog.cjs');
const root=path.resolve(__dirname,'..');
const original='D:/code/intellij/cangjie/';
const state=new Map(), initial=new Map(), history=new Map(), issues=[], skipped=[], applied=[];
function relative(file) {
  file=file.replaceAll('\\','/');
  if(file.toLowerCase().startsWith(original.toLowerCase())) file=file.slice(original.length);
  if (/^[A-Za-z]:|^\//.test(file) || file.split('/').includes('..')) throw Error('Outside recovery root: '+file);
  return file;
}
function excluded(file) {
  return /^(intellij-ide|deveco|external)\//.test(file) || file.startsWith('.workbuddy/');
}
function read(file) {
  if(!state.has(file)) {
    let value=null;
    const result=cp.spawnSync('git',['show','737514ba7e37ffd9e431afc2cc4bfcfb4c933ddb:'+file],{cwd:root,encoding:'utf8',maxBuffer:20000000});
    if(result.status===0)value=result.stdout.replace(/\r\n/g,'\n');
    state.set(file,value); initial.set(file,value);
  }
  return state.get(file);
}
function matchesAt(lines, chunk, at, trim=false) {
  return chunk.every((line,i)=>at+i<lines.length && (trim ? lines[at+i].trimEnd()===line.trimEnd() : lines[at+i]===line));
}
function locate(lines,chunk,start,hint=null) {
  if(chunk.length===0) return {at:hint??lines.length-(lines.at(-1)===''?1:0),mode:'append'};
  for(const trim of [false,true]) {
    const matches=[];
    for(let at=start;at<=lines.length-chunk.length;at++)if(matchesAt(lines,chunk,at,trim))matches.push(at);
    if(matches.length){if(hint!==null)matches.sort((a,b)=>Math.abs(a-hint)-Math.abs(b-hint));return{at:matches[0],mode:trim?'trailing-space':'exact'};}
  }
  return null;
}
function parse(patch) {
  const lines=patch.replace(/\r\n/g,'\n').split('\n');
  if(lines[0]!=='*** Begin Patch')throw Error('Not an apply_patch payload');
  const ops=[];let i=1;
  while(i<lines.length && lines[i]!=='*** End Patch') {
    const m=/^\*\*\* (Add|Update|Delete) File: (.+)$/.exec(lines[i]);
    if(!m){if(lines[i]===''){i++;continue;}throw Error('Bad operation '+lines[i]);}
    const op={kind:m[1],file:relative(m[2]),hunks:[]};i++;
    if(lines[i]?.startsWith('*** Move to: ')){op.move=relative(lines[i].slice(13));i++;}
    if(op.kind==='Add') {
      const content=[];
      while(i<lines.length&&!lines[i].startsWith('*** ')){if(!lines[i].startsWith('+'))throw Error('Bad add line');content.push(lines[i++].slice(1));}
      op.content=content.join('\n')+'\n';
    } else if(op.kind==='Update') {
      while(i<lines.length&&!/^\*\*\* (?:Update|Add|Delete|End Patch)/.test(lines[i])) {
        const numeric=/^@@ -(\d+)(?:,(\d+))? \+(\d+)(?:,(\d+))? @@/.exec(lines[i]);
        const oldStart=numeric?+numeric[1]:null;
        const anchor=!numeric&&lines[i].startsWith('@@ ')?lines[i].slice(3):null;
        if(lines[i].startsWith('@@'))i++;
        const before=[],after=[];
        while(i<lines.length&&!lines[i].startsWith('@@')&&!lines[i].startsWith('*** ')) {
          const line=lines[i++];if(line==='') {before.push('');after.push('');continue;}
          if(line[0]===' '){before.push(line.slice(1));after.push(line.slice(1));}
          else if(line[0]==='-')before.push(line.slice(1));
          else if(line[0]==='+')after.push(line.slice(1));
          else if(line==='\\ No newline at end of file')continue;
          else throw Error('Bad hunk line '+line);
        }
        const eof=lines[i]==='*** End of File';if(eof)i++;
        op.hunks.push({anchor,before,after,eof,oldStart});
      }
    }
    ops.push(op);
  }
  const names=ops.map(x=>x.file);if(new Set(names).size!==names.length)throw Error('Multiple operations on same file');
  return ops;
}
function apply(patch,call) {
  const ops=parse(patch);
  for(const op of ops) {
    const pending=[];
    try {
    if(excluded(op.file)){skipped.push({file:op.file,reason:'retained-nested-or-agent-scratch',...call});continue;}
    const old=read(op.file);
    if(call.session.startsWith('A') && call.time<'2026-09-28T23:49:32.000Z' && initial.get(op.file)!==null) {
      skipped.push({...call,file:op.file,reason:'already-in-checkpoint'});continue;
    }
    if(op.kind==='Add') {
      if(old!==null && old!==op.content)throw Error('Add exists: '+op.file);
      pending.push([op.file,op.content]);
    } else if(op.kind==='Delete') pending.push([op.file,null]);
    else {
      if(old===null)throw Error('Missing file: '+op.file);
      let lines=old.split('\n'), cursor=0, offset=0;
      for(const [index,hunk] of op.hunks.entries()) {
        if(hunk.anchor){const anchor=lines.findIndex((s,i)=>i>=cursor && s===hunk.anchor);if(anchor>=0)cursor=anchor+1;}
        const hint=hunk.oldStart===null||hunk.oldStart===undefined?null:hunk.oldStart-(hunk.before.length?1:0)+offset;
        let match=locate(lines,hunk.before,cursor,hint);
        if(!match) {
          const done=hunk.after.some(x=>x.trim())?locate(lines,hunk.after,0,hint):null;
          if(done){cursor=done.at+hunk.after.length;continue;}
          throw Error('Missing hunk '+index+' in '+op.file+' :: '+hunk.before.slice(0,5).join(' / '));
        }
        lines.splice(match.at,hunk.before.length,...hunk.after);cursor=match.at+hunk.after.length;
        offset+=hunk.after.length-hunk.before.length;
      }
      pending.push([op.move||op.file,lines.join('\n')]);
      if(op.move){read(op.move);pending.push([op.file,null]);}
    }
    for(const [file,value]of pending){state.set(file,value);history.set(file,[...(history.get(file)||[]),call]);}
    } catch(error) {issues.push({...call,file:op.file,reason:error.message});}
  }
  applied.push(call);
}
async function replay() {
  require('./snapshots.cjs').seed(read,state,history,apply);
  if(process.env.RECOVERY_LITERAL_MODE!=='1') {
    const {events,patch}=require('./events.cjs');
    const recorded=[...events(),...await require('./prehistory.cjs').prehistory()].sort((a,b)=>a.time.localeCompare(b.time)||a.line-b.line);
    for(const e of recorded) {
      for(const [file,change]of Object.entries(e.changes)) {
        const meta={session:e.session,line:e.line,time:e.time};
        try{apply(patch(file,change),meta);}catch(error){issues.push({...meta,file,reason:error.message});}
      }
    }
    return {state,initial,history,issues,skipped,applied};
  }
  for(const [index,c] of catalog().entries()) {
    const meta={index,session:c.session,line:c.line,time:c.time};
    if(c.session==='A-sub1'||c.session==='A-sub2')continue;
    if(c.failed && index!==114){skipped.push({...meta,reason:'recorded-failure',dynamic:c.dynamic.length});continue;}
    if([10,14,19,42,50,93,94,101,114,116].includes(index)) {
      try {await evaluateDynamic(c,meta);} catch(error) {issues.push({...meta,reason:'dynamic: '+error.message});}
      continue;
    }
    let restored=null;
    for(const data of ['a-main-dynamic.json','a-sub3-dynamic.json']) {
      const location=path.join(__dirname,data);
      if(fs.existsSync(location))restored=JSON.parse(fs.readFileSync(location,'utf8'))[index]||restored;
    }
    if(restored){for(const patch of restored)apply(patch,meta);continue;}
    if(c.dynamic.length)issues.push({...meta,reason:'dynamic-expression',literals:c.patches.length});
    for(const patch of c.patches) {
      try {apply(patch,meta);} catch(error) {issues.push({...meta,reason:error.message});}
    }
  }
  return {state,initial,history,issues,skipped,applied};
}
// 仅模拟已审阅的补丁表达式；exec_command 是内存读取适配器，不启动历史进程。
async function evaluateDynamic(c,meta) {
  const paths={oldImport:'psi/src/org/cangnova/cangjie/psi/CjImportDirectiveItem.kt',oldImportType:'psi/src/org/cangnova/cangjie/psi/stubs/elements/CjImportDirectiveItemElementType.kt'};
  const load=key=>{
    if(paths[key])return read(paths[key]).trimEnd();
    if(key==='parserOld') {
      const s=read('psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt');
      const start=s.lastIndexOf('    context(parseContext: ParsingContext)',s.indexOf('    private fun parseImportItem()'));
      const end=s.indexOf('    /**\n     * 提供 `parseOnlyAnnotationFile`',start);
      if(start<0||end<start)throw Error('parser region missing');
      return s.slice(start,end);
    }
    throw Error('Unknown historical stored value '+key);
  };
  const fakeTools={
    apply_patch:async patch=>{apply(patch,meta);return {};},
    exec_command:async args=>{
      const m=/^Get-Content -Raw -Encoding UTF8 (?:'([^']+)'|([^;|\s]+))$/.exec(args.cmd);
      if(!m)return {output:'',exit_code:0};
      const file=relative(m[1]||m[2]);const value=read(file);
      if(value===null)throw Error('Cannot read dynamic input '+file);
      return {output:value,exit_code:0};
    }
  };
  const code=meta.index===114?c.code.slice(0,c.code.indexOf('text(await tools.apply_patch("')):c.code;
  const context=vm.createContext({tools:fakeTools,load,text:()=>{}},{codeGeneration:{strings:false,wasm:false}});
  await new vm.Script('(async()=>{'+code+'})()').runInContext(context,{timeout:2000});
}
module.exports={replay,apply,read,relative,state,initial,history,parse,root};
if(require.main===module)(async()=>{
  await replay();
  const changed=[...state].filter(([file,value])=>value!==initial.get(file));
  if(process.argv[2]==='file')process.stdout.write(JSON.stringify({file:process.argv[3],content:state.get(process.argv[3]),original:initial.get(process.argv[3]),history:history.get(process.argv[3])}));
  else if(process.argv[2]==='patch') {
    const file=process.argv[3], value=state.get(file);
    let old=null;try{old=fs.readFileSync(path.join(root,file),'utf8').replace(/\r\n/g,'\n');}catch(e){if(e.code!=='ENOENT')throw e;}
    if(value===old)process.stdout.write('');
    else if(value===null)process.stdout.write('*** Begin Patch\n*** Delete File: '+path.join(root,file)+'\n*** End Patch');
    else if(old===null)process.stdout.write('*** Begin Patch\n*** Add File: '+path.join(root,file)+'\n'+value.trimEnd().split('\n').map(l=>'+'+l).join('\n')+'\n*** End Patch');
    else process.stdout.write('*** Begin Patch\n*** Update File: '+path.join(root,file)+'\n@@\n'+old.trimEnd().split('\n').map(l=>'-'+l).join('\n')+'\n'+value.trimEnd().split('\n').map(l=>'+'+l).join('\n')+'\n*** End Patch');
  }else process.stdout.write(JSON.stringify({changed:changed.map(([file,value])=>({file,bytes:value?.length??0,operations:history.get(file)?.length??0})),issues,applied:applied.length,skipped:skipped.length},null,2));
})();
