p = 'D:/code/intellij/cangjie/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt'
with open(p, 'rb') as f:
    t = f.read().decode('utf-8')

nl = '\r\n' if '\r\n' in t else '\n'
bad = '    }' + nl + '    }' + nl + nl + '}' + nl
# 文件末尾形态：matchDeclaration 的 `    }` 后多了 `    }`，随后是类闭合 `}`
bad2 = '    protected open fun matchDeclaration(declaration: CfirDeclaration, data: Nothing?) {' + nl + \
       '        CfirCjmpMatchRunner.matchDeclaration(declaration, session, storage)' + nl + \
       '    }' + nl + '    }' + nl + nl + '}' + nl
good = '    protected open fun matchDeclaration(declaration: CfirDeclaration, data: Nothing?) {' + nl + \
       '        CfirCjmpMatchRunner.matchDeclaration(declaration, session, storage)' + nl + \
       '    }' + nl + '}' + nl

if bad2 in t:
    t = t.replace(bad2, good, 1)
    with open(p, 'wb') as f:
        f.write(t.encode('utf-8'))
    print('stray brace removed')
else:
    print('pattern not found; dumping tail for manual fix')
    print(repr(t[-200:]))
