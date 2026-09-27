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


# 1) LLFlightRecorder：补 CJMP_MATCHING -> 17（未用编号，遵守"不变更既有映射"约定）
p = R + '/analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/util/LLFlightRecorder.kt'
t = read(p)
if 'CJMP_MATCHING ->' not in t:
    t = replace_once(t, '            CfirResolvePhase.IMPLICIT_TYPES -> 16',
                     '            CfirResolvePhase.IMPLICIT_TYPES -> 16\n            // 17 = CJMP_MATCHING（1.1.0 起；不得复用已废弃编号）\n            CfirResolvePhase.CJMP_MATCHING -> 17',
                     'flight recorder branch')
    write(p, t)
    print('LLFlightRecorder patched')
else:
    print('LLFlightRecorder already patched')

# 2) LL resolver：相位操作窄化到 CfirDeclaration
p = R + '/analysis/low-level-api-cfir/src/org/cangnova/cangjie/analysis/low/level/api/cfir/transformers/LLCfirCjmpMatchingLazyResolver.kt'
t = read(p)
if 'target is CfirDeclaration' not in t:
    old = '''        if (target.resolvePhase < CfirResolvePhase.CJMP_MATCHING) {
            target.replaceResolvePhase(CfirResolvePhase.CJMP_MATCHING)
        }'''
    new = '''        // 相位推进只对声明生效（file 等元素没有 resolvePhase/replaceResolvePhase）
        if (target is CfirDeclaration && target.resolvePhase < CfirResolvePhase.CJMP_MATCHING) {
            target.replaceResolvePhase(CfirResolvePhase.CJMP_MATCHING)
        }'''
    t = replace_once(t, old, new, 'phase advance narrow')
    if 'import org.cangnova.cangjie.cfir.declarations.CfirDeclaration\n' not in t:
        t = replace_once(t, 'import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration',
                         'import org.cangnova.cangjie.cfir.declarations.CfirDeclaration\nimport org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration',
                         'declaration import')
    write(p, t)
    print('LL resolver narrowed')
else:
    print('LL resolver already narrowed')
