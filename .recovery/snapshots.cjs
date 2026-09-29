// 从已完成工具输出中读取历史文本快照，不执行日志中的命令。
const fs=require('node:fs');
const {sources,textOf}=require('./catalog.cjs');
function eventAt(session,line) {
  const source=sources.find(x=>x[0]===session)[1];
  return JSON.parse(fs.readFileSync('C:/Users/lin17/.codex/sessions/2026/09/'+source,'utf8').split(/\r?\n/)[line-1]);
}
function outputAt(session,line) {
  const source=sources.find(x=>x[0]===session)[1];
  const es=fs.readFileSync('C:/Users/lin17/.codex/sessions/2026/09/'+source,'utf8').split(/\r?\n/).map(x=>{try{return JSON.parse(x);}catch{return {};}});
  const id=es[line-1].payload.call_id;
  const event=es.find(e=>e.payload?.call_id===id&&e.payload.type.endsWith('output'));
  const output=textOf(event.payload.output);
  return output.split('\n').flatMap(line=>{try {const x=JSON.parse(line);return typeof x.output==='string'?[x.output.replace(/\r\n/g,'\n')]:[];}catch{return [];}}).join('\n');
}
function seed(read,state,history,apply) {
  const text=outputAt('B-sub1',108);
  const resolverPackage='package org.cangnova.cangjie.cfir.resolve\n';
  const resolverStart=text.indexOf(resolverPackage);
  const psiStart=text.indexOf('/**\n * 提供 `isAbstract`');
  const helper='analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/references/CfirReferenceResolveHelper.kt';
  const original=read(helper);
  const tail='            diagnostic?.symbol?.buildSymbol(symbolBuilder)';
  const tailAt=original.indexOf(tail);
  if(tailAt<0||resolverStart<0||psiStart<0)throw Error('Invalid baseline snapshot markers');
  state.set(helper,text.slice(0,resolverStart).trimEnd()+original.slice(tailAt+tail.length));
  const resolver='cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/CfirImportBindingResolver.kt';
  const resolverBom=read(resolver).startsWith('\uFEFF')?'\uFEFF':'';
  state.set(resolver,resolverBom+text.slice(resolverStart,psiStart).trimEnd()+'\n');
  const psi='psi/src/org/cangnova/cangjie/psi/psiUtil/CjPsiUtil.kt';
  const oldPsi=read(psi);
  const start=oldPsi.lastIndexOf('/**',oldPsi.indexOf('fun CjSimpleNameExpression.isImportDirectiveExpression'));
  const end=oldPsi.indexOf('/**',start+3);
  const startSnapshot=text.indexOf('/**\n * 判断简单名是否属于 import item');
  const endSnapshot=text.indexOf('/**',startSnapshot+3);
  state.set(psi,oldPsi.slice(0,start)+text.slice(startSnapshot,endSnapshot)+oldPsi.slice(end));
  for(const file of [helper,resolver,psi])history.set(file,[{session:'B-sub1',line:108,reason:'historical pre-change snapshot'}]);
  // 记录的整文件读取保留了原始 dirty 基线，避免用 Git 版本替代原文。
  const source=sources.find(x=>x[0]==='B-main')[1];
  const events=fs.readFileSync('C:/Users/lin17/.codex/sessions/2026/09/'+source,'utf8').split(/\r?\n/).flatMap(x=>{try{return[JSON.parse(x)];}catch{return[];}});
  const itemFile='psi/src/org/cangnova/cangjie/psi/CjImportDirectiveItem.kt';
  const raw=events.find(e=>e.payload?.item?.type==='CommandExecution'&&e.payload.item.status==='completed'&&e.timestamp<'2026-09-28T08:00:52Z'&&e.payload.item.command?.at(-1)==='Get-Content -Raw -Encoding UTF8 '+itemFile);
  if(!raw)throw Error('Missing original import PSI snapshot');
  read(itemFile);state.set(itemFile,raw.payload.item.stdout.replace(/\r\n/g,'\n').trimEnd()+'\n');
  history.set(itemFile,[{session:'B-main',time:raw.timestamp,reason:'historical raw file snapshot'}]);
  const planning='analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirImportPlanning.kt';
  const headerOutput=outputAt('B-sub3',181);
  const header=headerOutput.slice(headerOutput.indexOf('package org.cangnova.cangjie.analysis.api.cfir.components'));
  const oldPlanning=read(planning);
  state.set(planning,header.slice(0,header.indexOf('/**'))+oldPlanning.slice(oldPlanning.indexOf('/**')));
  const diff=eventAt('B-sub3',274).payload.item.stdout.replace(/\r\n/g,'\n');
  const begin=diff.indexOf('@@ -304,17 +309,25');
  const diffEnd=diff.indexOf('\n@@ ',begin+3);
  const hunk=diff.slice(begin,diffEnd).replace(/^@@[^\n]*$/gm,'@@');
  apply('*** Begin Patch\n*** Update File: '+planning+'\n'+hunk+'\n*** End Patch',{session:'B-sub3',line:274,time:'2026-09-28T07:42:00Z',reason:'pre-existing dirty scope lookup from recorded git diff'});
}
module.exports={outputAt,eventAt,seed};
if(require.main===module)process.stdout.write(outputAt(process.argv[2],+process.argv[3]));
