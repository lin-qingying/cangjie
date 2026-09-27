p = 'D:/code/intellij/cangjie/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt'
with open(p, 'rb') as f:
    t = f.read().decode('utf-8')

if 'CfirCjmpMatchRunner' in t:
    print('already delegated')
else:
    start = t.find('    protected open fun matchDeclaration(declaration: CfirDeclaration, data: Nothing?) {')
    assert start != -1, 'matchDeclaration not found'
    # 找到该函数块的结束（下一个顶格 '    }' 后再接 '}' 前的函数结束）
    end_marker = '\n    }\n\n    /**\n     * 已配对容器内的成员配对'
    end = t.find(end_marker, start)
    assert end != -1, 'matchMembers start not found'
    new_decl = '''    protected open fun matchDeclaration(declaration: CfirDeclaration, data: Nothing?) {
        CfirCjmpMatchRunner.matchDeclaration(declaration, session, storage)
    }'''
    t = t[:start] + new_decl + t[end:]

    # 删除 matchMembers 整块直至类的结束（runner 已承接）
    members_start = t.find('''    /**
     * 已配对容器内的成员配对''')
    assert members_start != -1, 'matchMembers doc not found'
    close = t.find('\n}\n', members_start)
    assert close != -1, 'class close not found'
    t = t[:members_start] + '}\n'

    for imp in [
        'import org.cangnova.cangjie.cfir.declarations.CfirConstructor\n',
        'import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor\n',
        'import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration\n',
        'import org.cangnova.cangjie.cfir.declarations.CfirProperty\n',
        'import org.cangnova.cangjie.cfir.declarations.CfirStruct\n',
        'import org.cangnova.cangjie.cfir.declarations.CfirInterface\n',
        'import org.cangnova.cangjie.cfir.declarations.CfirEnum\n',
        'import org.cangnova.cangjie.cfir.declarations.CfirClass\n',
        'import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatcher\n',
        'import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpResolver\n',
        'import org.cangnova.cangjie.cfir.resolve.cjmp.CjmpMatchResult\n',
    ]:
        if t.count(imp) == 1:
            t = t.replace(imp, '', 1)
    imp_anchor = 'import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMappingStorage'
    t = t.replace(imp_anchor, imp_anchor + '\nimport org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner', 1)

    with open(p, 'wb') as f:
        f.write(t.encode('utf-8'))
    print('processor delegated')
