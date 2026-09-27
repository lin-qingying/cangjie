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
    assert c == 1, f"{what}: anchor count {c}"
    return text.replace(old2, new2, 1)


# 1) matcher: 形参类型属性 returnTypeRef
p = R + '/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatcher.kt'
t = read(p)
t = replace_once(t,
                 'specificParameter.typeRef.coneTypeOrNull(),\n                    commonParameter.typeRef.coneTypeOrNull(),',
                 'specificParameter.returnTypeRef.coneTypeOrNull(),\n                    commonParameter.returnTypeRef.coneTypeOrNull(),',
                 'matcher param types')
write(p, t)
print('matcher fixed')

# 2) resolver: status 需经 CfirMemberDeclaration 窄化
p = R + '/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpResolver.kt'
t = read(p)
t = replace_once(t, 'import org.cangnova.cangjie.cfir.declarations.CfirDeclaration',
                 'import org.cangnova.cangjie.cfir.declarations.CfirDeclaration\nimport org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration',
                 'resolver import')
t = replace_once(t, '.filter { it.status.isCommon && it.moduleData.name in dependencyNames }',
                 '.filter { candidate ->\n                (candidate as? CfirMemberDeclaration)?.status?.isCommon == true &&\n                        candidate.moduleData.name in dependencyNames\n            }',
                 'resolver filter')
write(p, t)
print('resolver fixed')

# 3) processor: 恢复遍历钩子所需导入
p = R + '/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt'
t = read(p)
needed = [
    'import org.cangnova.cangjie.cfir.declarations.CfirClass\n',
    'import org.cangnova.cangjie.cfir.declarations.CfirConstructor\n',
    'import org.cangnova.cangjie.cfir.declarations.CfirEnum\n',
    'import org.cangnova.cangjie.cfir.declarations.CfirInterface\n',
    'import org.cangnova.cangjie.cfir.declarations.CfirProperty\n',
    'import org.cangnova.cangjie.cfir.declarations.CfirStruct\n',
]
anchor = 'import org.cangnova.cangjie.cfir.declarations.CfirFile'
assert t.count(anchor) == 1, f"processor anchor {t.count(anchor)}"
missing = [imp for imp in needed if imp not in t]
if missing:
    t = t.replace(anchor, ''.join(missing) + anchor, 1)
write(p, t)
print('processor imports restored:', len(missing))
