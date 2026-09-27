import re
root = 'D:/code/intellij/cangjie/'
files = [
    'cfir/cfir-serialization/test/org/cangnova/cangjie/cfir/serialization/cjd/CjdBinaryFixture.kt',
    'analysis/decompiled/decompiler-to-stubs/test/org/cangnova/cangjie/analysis/decompiler/stub/CjdDeclarationLoaderIntegrationTest.kt',
]
for f in files:
    p = root + f
    s = open(p, encoding='utf-8', newline='').read()
    nl = '\r\n' if '\r\n' in s else '\n'
    m = re.search(r'([ \t]*)Package\.startPackage\(builder\)' + re.escape(nl), s)
    ind = m.group(1)
    add = (f"{ind}// 官方加载管线拒绝缺失 cjoVersion 的 cjo（ASTLoader::CheckCjoVersion）{nl}"
           f"{ind}Package.addCjoVersion({nl}"
           f"{ind}    builder,{nl}"
           f"{ind}    PackageFormat.CjoVersion.createCjoVersion({nl}"
           f"{ind}        builder,{nl}"
           f"{ind}        org.cangnova.cangjie.cfir.serialization.CjoConstants.VERSION_MAJOR.toUByte(),{nl}"
           f"{ind}        org.cangnova.cangjie.cfir.serialization.CjoConstants.VERSION_MINOR.toUByte(),{nl}"
           f"{ind}        org.cangnova.cangjie.cfir.serialization.CjoConstants.VERSION_PATCH.toUByte(),{nl}"
           f"{ind}    ),{nl}"
           f"{ind}){nl}")
    s = s[:m.end()] + add + s[m.end():]
    open(p, 'w', encoding='utf-8', newline='').write(s)
    print('ok', f)
