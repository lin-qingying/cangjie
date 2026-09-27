p = 'D:/code/intellij/cangjie/cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirDefaultSessionFactory.kt'
with open(p, 'rb') as f:
    t = f.read().decode('utf-8')
imp = 'import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMappingStorage\n'
if imp in t and 'CfirCjmpMappingStorage(' not in t:
    t = t.replace(imp, '', 1)
    with open(p, 'wb') as f:
        f.write(t.encode('utf-8'))
    print('unused import removed')
else:
    print('no change (or still used)')
