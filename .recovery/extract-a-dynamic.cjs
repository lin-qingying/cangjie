// 从已完成 CommandExecution 输出重建动态补丁；不执行任何历史命令、不写源码。
const fs = require('node:fs');
const vm = require('node:vm');
const {catalog} = require('./catalog.cjs');
const {parse} = require('./replay.cjs');
const calls = catalog();
const requested = new Map([[150,480],[170,859],[172,919]]);
const normalize = s => String(s).replace(/\r\n/g, '\n');

function applyRecordedDiff(original, unifiedDiff) {
  const lines=normalize(original).split('\n'), diff=normalize(unifiedDiff).replace(/\n$/, '').split('\n');
  let i=0, shift=0;
  while(i<diff.length) {
    const header=/^@@ -(\d+)(?:,\d+)? \+\d+(?:,\d+)? @@/.exec(diff[i++]);
    if(!header)throw Error('Unexpected recorded diff header');
    const before=[],after=[];
    while(i<diff.length&&!diff[i].startsWith('@@ ')) {
      const line=diff[i++];
      if(line.startsWith('\\ No newline'))continue;
      if(line[0]===' '){before.push(line.slice(1));after.push(line.slice(1));}
      else if(line[0]==='-')before.push(line.slice(1));
      else if(line[0]==='+')after.push(line.slice(1));
      else throw Error('Unexpected recorded diff line');
    }
    const at=Number(header[1])-1+shift;
    if(!before.every((line,n)=>lines[at+n]===line))throw Error('Recorded diff preimage mismatch');
    lines.splice(at,before.length,...after);
    shift+=after.length-before.length;
  }
  return lines.join('\n');
}

async function extract() {
  const patches = {}, audit = [];
  for (const [index, expectedLine] of requested) {
    const call = calls[index];
    if (call.session !== 'A-main' || call.line !== expectedLine || call.failed) throw Error('Catalog mismatch: ' + index);
    const events = fs.readFileSync(call.file, 'utf8').split(/\r?\n/).map((line, i) => {
      try {return {...JSON.parse(line), line:i+1};} catch {return null;}
    }).filter(Boolean);
    const finish = events.find(e => e.line > call.line && e.payload?.call_id === call.id && e.payload.type.endsWith('_output'));
    if (!finish) throw Error('Missing result ' + index);
    const interval = events.filter(e => e.line > call.line && e.line < finish.line);
    const reads = interval.filter(e => e.payload?.item?.type === 'CommandExecution' && /^Get-Content -LiteralPath /.test(e.payload.item.command.at(-1)));
    const changes = interval.filter(e => e.payload?.item?.type === 'FileChange');
    if (changes.length !== 1 || changes[0].payload.item.status !== 'completed') throw Error('Missing successful FileChange ' + index);
    const seen = new Set(), captured = [], skippedCommands = [], stored = new Map();
    const mockTools = Object.freeze({
      exec_command: async args => {
        if (!/^Get-Content -LiteralPath /.test(args.cmd)) {
          skippedCommands.push(args.cmd);
          return {exit_code:0, output:''};
        }
        const event = reads.find(e => e.payload.item.command.at(-1) === args.cmd && !seen.has(e.line));
        if (!event || event.payload.item.exit_code !== 0) throw Error('Missing successful recorded read: ' + args.cmd);
        seen.add(event.line);
        const item = event.payload.item;
        return {exit_code:item.exit_code, output:item.aggregated_output ?? item.stdout};
      },
      apply_patch: async patch => {captured.push(patch); return {};},
    });
    await vm.runInNewContext('(async () => {\n' + call.code + '\n})()', {
      tools:mockTools,
      store:(key,value)=>stored.set(key,value), load:key=>stored.get(key), text:()=>{},
    }, {timeout:3000, contextCodeGeneration:{strings:false,wasm:false}});
    if (captured.length !== 1 || seen.size !== reads.length) throw Error('Capture/read count mismatch ' + index);
    const ops = captured.flatMap(parse);
    const changeEntries = Object.entries(changes[0].payload.item.changes);
    if (ops.length !== changeEntries.length) throw Error('File count mismatch ' + index);
    for (const op of ops) {
      const entry = changeEntries.find(([file]) => normalize(file).replaceAll('\\','/').endsWith('/'+op.file));
      if (!entry) throw Error('Patch is not in successful FileChange: ' + op.file);
      const [, change] = entry;
      if (op.kind === 'Add') {
        if (change.type !== 'add' || normalize(change.content) !== op.content) throw Error('Add content mismatch '+op.file);
      } else {
        if (change.type !== 'update' || op.hunks.length !== 1) throw Error('Unexpected dynamic update '+op.file);
        const read = reads.find(e => e.payload.item.command.at(-1).includes(op.file));
        const original = normalize(read.payload.item.aggregated_output ?? read.payload.item.stdout).trimEnd();
        if (original !== op.hunks[0].before.join('\n')) throw Error('Historical baseline mismatch '+op.file);
        const actual = applyRecordedDiff(read.payload.item.aggregated_output ?? read.payload.item.stdout, change.unified_diff);
        if (actual.trimEnd() !== op.hunks[0].after.join('\n').trimEnd()) throw Error('Successful FileChange result mismatch '+op.file);
      }
    }
    patches[index] = captured;
    audit.push({index,callLine:call.line,readLines:[...seen].sort((a,b)=>a-b),successLine:changes[0].line,resultLine:finish.line,
      files:ops.map(x=>x.file),patchChars:captured[0].length,skippedCommands});
  }
  return {patches,audit};
}
module.exports={extract};
if (require.main === module) extract().then(result => process.stdout.write(JSON.stringify(process.argv.includes('--json')?result.patches:result.audit,null,2))).catch(error=>{console.error(error);process.exitCode=1;});
