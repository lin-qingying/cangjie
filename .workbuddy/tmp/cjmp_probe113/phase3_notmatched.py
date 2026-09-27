import os
import time

R = 'D:/code/intellij/cangjie'


def read(p):
    with open(p, 'rb') as f:
        return f.read().decode('utf-8')


def robust_write(path, text):
    data = text.encode('utf-8')
    tmp = path + '.cjmp.tmp'
    last = None
    for _ in range(20):
        try:
            with open(tmp, 'wb') as f:
                f.write(data)
            os.replace(tmp, path)
            return
        except OSError as e:
            last = e
            time.sleep(0.5)
    raise last


def replace_once(text, old, new, what):
    if text.count(old) == 1:
        return text.replace(old, new, 1)
    nl = '\r\n' if '\r\n' in text else '\n'
    old2, new2 = old.replace('\n', nl), new.replace('\n', nl)
    c = text.count(old2)
    assert c == 1, f"{what}: anchor count {c} (lf={text.count(old)})"
    return text.replace(old2, new2, 1)


# 0) 预检：NOT_MATCHED 的其他调用点
import subprocess
out = subprocess.run(
    ['grep', '-rn', 'CfirErrors.NOT_MATCHED', R + '/cfir/checkers/src', R + '/cfir/resolve/src', R + '/analysis'],
    capture_output=True, text=True)
print('NOT_MATCHED call sites:')
print(out.stdout)

# 1) 生成器：NOT_MATCHED 参数形改为 3×String（官方 CheckCJMP.cpp:156 语义）
p = R + '/cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/diagnostics/CfirDiagnosticsList.kt'
t = read(p)
old = '''        val NOT_MATCHED by error<PsiElement> {
            parameter<Name>("declarationName")
            parameter<String>("kind")
            parameter<String>("matchKind")
        }'''
new = '''        val NOT_MATCHED by error<PsiElement> {
            // 官方参数形（CheckCJMP.cpp:156）：(side, "Kind 'name'", counterpartSide)
            parameter<String>("side")
            parameter<String>("declarationInfo")
            parameter<String>("counterpartKind")
        }'''
t = replace_once(t, old, new, 'NOT_MATCHED params')
robust_write(p, t)
print('generator NOT_MATCHED params updated')

# 2) 消息渲染器：第一参改 RENDER_STRING
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt'
t = read(p)
old = '        map.put(CfirErrors.NOT_MATCHED, "\'\'{0}\'\' {1} can not find \'\'{2}\'\' match", RENDER_NAME, RENDER_STRING, RENDER_STRING)'
new = '        map.put(CfirErrors.NOT_MATCHED, "\'\'{0}\'\' {1} can not find \'\'{2}\'\' match", RENDER_STRING, RENDER_STRING, RENDER_STRING)'
t = replace_once(t, old, new, 'NOT_MATCHED renderers')
robust_write(p, t)
print('message renderers updated')

# 3) 检查器：两处调用点改传字符串 + 非穷尽枚举规则接线
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt'
t = read(p)

old1 = '''                    factory = CfirErrors.NOT_MATCHED,
                    a = "specific",
                    b = "${declKindText(specificDecl)} '${specificDecl.name.asString()}'",
                    c = "common",'''
new1 = '''                    factory = CfirErrors.NOT_MATCHED,
                    a = "specific",
                    b = "${declKindText(specificDecl)} '${specificDecl.name.asString()}'",
                    c = "common",'''
# 已一致，无需改（参数类型同步后即通过）

old2 = '''                    factory = CfirErrors.NOT_MATCHED,
                        a = "specific",'''
# 成员方向调用点（缩进更深）保持；参数类型同步后即通过

# 3a) 非穷尽枚举接线（C32）
anchor_enum = '''        // 检查成员匹配
        checkMemberMatch(specificDecl, commonDecl)
    }'''
new_enum = '''        // 非穷尽枚举不匹配（C32 接线，官方 MatchCJMPDecls 面）：
        // common 穷尽（无 `...`）时 specific 不得为非穷尽。
        if (specificDecl is CfirEnum && commonDecl is CfirEnum &&
            !commonDecl.isNonExhaustive && specificDecl.isNonExhaustive
        ) {
            reporter.reportOn(
                source = specificDecl.source,
                factory = CfirErrors.COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH,
                a = declKindText(commonDecl),
                b = declKindText(specificDecl),
            )
        }

        // 检查成员匹配
        checkMemberMatch(specificDecl, commonDecl)
    }'''
t = replace_once(t, anchor_enum, new_enum, 'enum non-exhaustive rule')
robust_write(p, t)
print('checker updated (enum rule)')

# 4) psi2cfir isNonExhaustive 路径核查
out2 = subprocess.run(
    ['grep', '-rn', 'isNonExhaustive', R + '/cfir/raw-cfir/psi2cfir/src'],
    capture_output=True, text=True)
print('psi2cfir isNonExhaustive:', out2.stdout.strip() or '(none)')
