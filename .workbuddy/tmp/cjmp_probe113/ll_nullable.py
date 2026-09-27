R = 'D:/code/intellij/cangjie'


def read(p):
    with open(p, 'rb') as f:
        return f.read().decode('utf-8')


def write(p, t):
    with open(p, 'wb') as f:
        f.write(t.encode('utf-8'))


def replace_once(text, old, new, what):
    if text.count(old) == 1:
        return text.replace(old, new, 1)
    nl = '\r\n' if '\r\n' in text else '\n'
    old2, new2 = old.replace('\n', nl), new.replace('\n', nl)
    c = text.count(old2)
    assert c == 1, f"{what}: anchor count {c} (lf={text.count(old)})"
    return text.replace(old2, new2, 1)


p = R + '/analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpMatchingLazyResolver.kt'
t = read(p)
if 'cjmpMappingStorageOrNull' not in t:
    t = replace_once(t, 'import org.cangnova.cangjie.cfir.resolve.cjmp.cjmpMappingStorage',
                     'import org.cangnova.cangjie.cfir.resolve.cjmp.cjmpMappingStorageOrNull', 'll import')
    old = '''        if (enabled && target is CfirMemberDeclaration) {
            if (CfirCjmpMatchRunner.canHaveCommonCounterpart(target)) {
                CfirCjmpMatchRunner.matchDeclaration(target, resolveTargetSession, resolveTargetSession.cjmpMappingStorage)
            }
        }'''
    new = '''        if (enabled && target is CfirMemberDeclaration) {
            if (CfirCjmpMatchRunner.canHaveCommonCounterpart(target)) {
                // 轻量会话（平台 common/桩会话）可能未装配存储：按"无配对数据"处理
                resolveTargetSession.cjmpMappingStorageOrNull?.let { storage ->
                    CfirCjmpMatchRunner.matchDeclaration(target, resolveTargetSession, storage)
                }
            }
        }'''
    t = replace_once(t, old, new, 'll storage access')
    write(p, t)
    print('ll resolver switched to nullable')
else:
    print('ll resolver already nullable')
