p = 'D:/code/intellij/cangjie/psi/test/org/cangnova/cangjie/psi/ModifierParsingTest.kt'
with open(p, 'rb') as f:
    t = f.read().decode('utf-8')

nl = '\r\n' if '\r\n' in t else '\n'

old = '''        val func = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java).single()
        assertTrue(func.hasModifier(CjTokens.COMMON_KEYWORD))'''.replace('\n', nl)
new = '''        // 文件内有两个命名函数（class 成员 f 与 interface 成员 g），按名选取断言
        val classMember = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
            .single { it.name == "f" }
        assertTrue(classMember.hasModifier(CjTokens.COMMON_KEYWORD))
        val interfaceMember = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
            .single { it.name == "g" }
        assertTrue(!interfaceMember.hasModifier(CjTokens.COMMON_KEYWORD))'''.replace('\n', nl)

assert t.count(old) == 1, f"anchor count {t.count(old)}"
t = t.replace(old, new, 1)
with open(p, 'wb') as f:
    f.write(t.encode('utf-8'))
print('psi test fixed')
