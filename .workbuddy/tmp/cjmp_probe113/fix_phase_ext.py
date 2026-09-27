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

# 1) 撤销窄化（resolvePhase/replaceResolvePhase 是 CfirElementWithResolveState 的扩展）
old = '''        // 相位推进只对声明生效（file 等元素没有 resolvePhase/replaceResolvePhase）
        if (target is CfirDeclaration && target.resolvePhase < CfirResolvePhase.CJMP_MATCHING) {
            target.replaceResolvePhase(CfirResolvePhase.CJMP_MATCHING)
        }'''
new = '''        if (target.resolvePhase < CfirResolvePhase.CJMP_MATCHING) {
            target.replaceResolvePhase(CfirResolvePhase.CJMP_MATCHING)
        }'''
t = replace_once(t, old, new, 'unnarrow phase ops')

# 2) 补扩展导入
if 'import org.cangnova.cangjie.cfir.declarations.resolvePhase\n' not in t:
    t = replace_once(t, 'import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase',
                     'import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase\nimport org.cangnova.cangjie.cfir.declarations.replaceResolvePhase\nimport org.cangnova.cangjie.cfir.declarations.resolvePhase',
                     'extension imports')
# 清理不再使用的 CfirDeclaration 导入（若确未使用）
if 'CfirDeclaration\b' not in t.split('import org.cangnova.cangjie.cfir.declarations.CfirDeclaration\n')[1].replace('CfirDeclaration', 'X'):
    pass
write(p, t)
print('LL resolver extensions fixed')
