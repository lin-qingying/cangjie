// 专项只读检查 workspace.xml 候选；复用已验证 VFS 解码，不调用 IDE 存储 API。
const fs=require('node:fs'), crypto=require('node:crypto');
const {content}=require('./localhistory.cjs');
const file='C:/Users/lin17/AppData/Local/JetBrains/IntelliJIdea2026.2/caches/content.dat';
const fd=fs.openSync(file,'r'), head=Buffer.alloc(64);
fs.readSync(fd,head,0,64,0);
const committedUpTo=Number(head.readBigUInt64LE(24));
const physicalBytes=fs.fstatSync(fd).size;
if(committedUpTo<64||committedUpTo>physicalBytes)throw Error('Invalid committed boundary');
const report={file,scannedAtUtc:new Date().toISOString(),physicalBytes,committedUpTo,minContentBytes:50*1024,maxContentBytes:300*1024,
  records:0,padding:0,sizeCandidates:0,hashVerified:0,decodeFailures:[],workspaceXmlRecords:0,unrelatedWorkspaceXmlCount:0,candidates:[],
  warning:'Content records do not provide a file path or timestamp; matching project markers establish a candidate only, not the final pre-reset revision.'};
const markers=[
  ['changeListUuid','7e15012e-e26c-4b45-a5dc-4283160ad6fc'],
  ['absoluteProjectPath','D:/code/intellij/cangjie'],
  ['absoluteProjectWindowsPath','D:\\code\\intellij\\cangjie'],
  ['importGroupPath','CjImportGroup'],
  ['projectAnalysisPath','cfir/analysis-tests'],
  ['projectPlanPath','cjmp-framework-review-and-fix-plan'],
];
const header=Buffer.alloc(28);
for(let offset=64;offset+4<=committedUpTo;){
  fs.readSync(fd,header,0,Math.min(28,committedUpTo-offset),offset);
  const raw=header.readUInt32LE(0),total=raw&0x3fffffff,type=raw>>>30;
  if(total<4||offset+total>committedUpTo)throw Error('Invalid record boundary at '+offset);
  if(type===3)report.padding++;
  else if(type===1){
    report.records++;
    if(total>=28){
      const size=Math.abs(header.readInt32LE(24));
      if(size>=report.minContentBytes&&size<=report.maxContentBytes){
        report.sizeCandidates++;
        const contentId=(offset-64)/4+1;
        try{
          const bytes=content(contentId);report.hashVerified++;
          const text=bytes.toString('utf8');
          if(text.includes('<component name="ChangeListManager">')&&/^\s*(<\?xml[^>]*>\s*)?<project\b/.test(text)&&/<\/project>\s*$/.test(text)){
            report.workspaceXmlRecords++;
            const matches=markers.filter(([,needle])=>text.includes(needle)).map(([name])=>name);
            if(matches.length){
              report.candidates.push({contentId,recordOffset:offset,bytes:bytes.length,sha256:crypto.createHash('sha256').update(bytes).digest('hex'),matchedProjectMarkers:matches,
                finalPreResetVersionVerified:false});
            }else report.unrelatedWorkspaceXmlCount++;
          }
        }catch(error){report.decodeFailures.push({contentId,recordOffset:offset,error:String(error.message)});}
      }
    }
  }else throw Error('Unexpected record flags '+type+' at '+offset);
  offset=(offset+total+3)&~3;
}
fs.closeSync(fd);
process.stdout.write(JSON.stringify(report,null,2));
