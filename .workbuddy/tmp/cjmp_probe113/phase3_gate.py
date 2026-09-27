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


# 1) CjmpGate：增补非上报型单入口 isEnabled
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CjmpGate.kt'
t = read(p)
if 'fun isEnabled' not in t:
    old = '''    /**
     * 模式门（D10 判定序第 ② 层）：`mode == None` 时报 `parse_unexpected_cjmp_decl`'''
    new = '''    /**
     * 版本门（非上报型）：仅返回判定结果，不产生诊断。
     *
     * 供"整族静默跳过"的既有检查器消费（它们本就不产出版本诊断；族级版本诊断由
     * `CfirCjmpParseRulesChecker` 按声明统一负责，避免重复上报）。
     */
    fun isEnabled(context: CheckerContext): Boolean =
        context.languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations)

    /**
     * 模式门（D10 判定序第 ② 层）：`mode == None` 时报 `parse_unexpected_cjmp_decl`'''
    t = replace_once(t, old, new, 'isEnabled add')
    write(p, t)
    print('CjmpGate.isEnabled added')
else:
    print('CjmpGate.isEnabled exists')

# 2) CfirCommonCtorImmutableAssignChecker：散写门禁改走 CjmpGate
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonCtorImmutableAssignChecker.kt'
t = read(p)
if 'CjmpGate' not in t:
    old = '''        // 与 CfirCommonSpecificChecker 同一门禁：common/specific 语言表面 1.1.0 起才存在。
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations)) return'''
    new = '''        // 与 CfirCommonSpecificChecker 同一门禁：common/specific 语言表面 1.1.0 起才存在（D16 单入口）。
        if (!CjmpGate.isEnabled(context)) return'''
    t = replace_once(t, old, new, 'ctor checker gate')
    imp_anchor = 'import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext'
    if 'import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate' not in t and imp_anchor in t:
        t = t.replace(imp_anchor, 'import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate\n' + imp_anchor, 1)
    write(p, t)
    print('ctor checker gate unified')
else:
    print('ctor checker already uses CjmpGate')

# 3) CfirCommonPackageMainChecker：补门禁（G12 逃逸点）
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt'
t = read(p)
if 'CjmpGate.isEnabled(context)' not in t:
    old = '''    override fun check(declaration: org.cangnova.cangjie.cfir.declarations.CfirFile) {
        for (decl in declaration.declarations) {'''
    new = '''    override fun check(declaration: org.cangnova.cangjie.cfir.declarations.CfirFile) {
        // 版本门（G12 逃逸修复）：非 1.1+ 语义下 common/specific 整族静默。
        if (!CjmpGate.isEnabled(context)) return
        for (decl in declaration.declarations) {'''
    t = replace_once(t, old, new, 'main checker gate')
    write(p, t)
    print('main checker gated')
else:
    print('main checker already gated')
