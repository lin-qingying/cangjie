// 将所有已验证的同项目 workspace 候选无损打包；不选定任何一个候选作为最终版本。
const crypto=require('node:crypto'),zlib=require('node:zlib');
const metadata=require('./workspace-vfs-candidates.json');
const {content}=require('./localhistory.cjs');
const records=metadata.candidates.map(record=>{
  const bytes=content(record.contentId);
  if(crypto.createHash('sha256').update(bytes).digest('hex')!==record.sha256)throw Error('Candidate hash changed');
  return {...record,contentBase64:bytes.toString('base64')};
});
const archive=zlib.brotliCompressSync(Buffer.from(JSON.stringify({format:'workspace-vfs-candidates-v1',records})),{params:{[zlib.constants.BROTLI_PARAM_QUALITY]:6,[zlib.constants.BROTLI_PARAM_LGWIN]:24}});
const data=archive.toString('base64');
if(process.argv[2]==='info')process.stdout.write(JSON.stringify({candidates:records.length,compressedBytes:archive.length,base64Length:data.length,sha256:crypto.createHash('sha256').update(archive).digest('hex')}));
else process.stdout.write(JSON.stringify({offset:+process.argv[2]||0,total:data.length,chunk:data.slice(+process.argv[2]||0,(+process.argv[2]||0)+60000)}));
