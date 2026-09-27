p = 'D:/code/intellij/cangjie/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatcher.kt'
with open(p, 'rb') as f:
    t = f.read().decode('utf-8')
old = 'import org.cangnova.cangjie.cfir.types.ConeTypeParameterType\n'
new = 'import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType\n'
assert t.count(old) == 1, f"import anchor {t.count(old)}"
t = t.replace(old, new, 1)
with open(p, 'wb') as f:
    f.write(t.encode('utf-8'))
print('ConeTypeParameterType import fixed')
