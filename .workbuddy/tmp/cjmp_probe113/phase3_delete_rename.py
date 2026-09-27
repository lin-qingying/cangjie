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


# 1) CjmpGate：补 languageVersionSettings 导入
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CjmpGate.kt'
t = read(p)
if 'import org.cangnova.cangjie.cfir.session.languageVersionSettings' not in t:
    t = replace_once(t, 'import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter',
                     'import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter\nimport org.cangnova.cangjie.cfir.session.languageVersionSettings',
                     'gate import')
    write(p, t)
    print('CjmpGate import added')
else:
    print('CjmpGate import exists')

# 2) 诊断清单：删除条目
p = R + '/cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/diagnostics/CfirDiagnosticsList.kt'
t = read(p)
old = '''        // common/specific 泛型重命名暂不支持
        val COMMON_GENERIC_RENAME_NOT_SUPPORTED by error<PsiElement>()

'''
if old in t:
    t = t.replace(old, '', 1)
    write(p, t)
    print('diagnostics list entry removed')
else:
    print('diagnostics list entry not found (check manually)')

# 3) 消息映射：删除条目
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt'
t = read(p)
old = '        map.put(CfirErrors.COMMON_GENERIC_RENAME_NOT_SUPPORTED, "common/specific generic rename is not supported yet")\n'
if old in t:
    t = t.replace(old, '', 1)
    write(p, t)
    print('message entry removed')
else:
    print('message entry not found')

# 4) checker：删除规则函数与调用点
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt'
t = read(p)

call = '''            checkCommonSpecificGenericConstraints(declaration)
'''
if call in t:
    t = t.replace(call, '', 1)
    print('call site removed')
else:
    print('call site not found')

fn_start = '''    /**
     * common/specific 泛型约束：
     * - 不支持 @Frozen 标注的泛型（已在 checkCommonExtraConstraints 处理）
     * - common 和 specific 的泛型参数不能重命名
     */'''
i = t.find(fn_start)
if i != -1:
    j = t.find('\n    }\n', i)
    assert j != -1, 'function end not found'
    t = t[:i] + t[j + len('\n    }\n'):]
    # 清理多余空行
    t = t.replace('\n\n\n', '\n\n')
    print('rule function removed')
else:
    print('rule function not found')

write(p, t)
print('checker cleaned')

# 5) testData 引用核查
import subprocess
out = subprocess.run(['grep', '-rn', 'COMMON_GENERIC_RENAME_NOT_SUPPORTED',
                      R + '/cfir/analysis-tests/testData'],
                     capture_output=True, text=True)
print('testData refs:', out.stdout.strip() or '(none)')
