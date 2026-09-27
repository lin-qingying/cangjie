import os
base = 'D:/code/intellij/cangjie/cfir/analysis-tests/testData/diagnostics2/common-specific/gate/'
os.makedirs(base, exist_ok=True)
files = {
    'cjmpModeNoneCommonFirst.cj': '''// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 模式门反例（计划 §8.4）：既非 common 也非 specific 编译（无 CJMP_MODE）。
// cjc 1.1.3 实测（`cjc none2.cj --experimental`）：只报一条文件级诊断，锚 package 行 1:1，
// 取源码顺序上首个错位声明���此处 common 在前）；其余 CJMP 规则不执行。
<!PARSE_COMMON_IN_NON_COMMON_FILE!>package cjmp_p<!>

public common func a(): Int64 { 1 }
public specific func b(): Int64 { 1 }
''',
    'cjmpModeNoneSpecificFirst.cj': '''// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 模式门反例（计划 §8.4）：同上，specific 在前时报 specific 方向诊断（cjc 1.1.3 实测 none3.cj）。
<!PARSE_SPECIFIC_IN_NON_SPECIFIC_FILE!>package cjmp_p<!>

public specific func b(): Int64 { 1 }
public common func a(): Int64 { 1 }
''',
    'cjmpFeatureDisabledOverride.cj': '''// LANGUAGE_VERSION: 1.1.0
// LANGUAGE: -CommonSpecificDeclarations
// CJMP_MODE: SPECIFIC
// RUN_PIPELINE_TILL: FRONTEND
// 特性覆盖反例（计划 §8.4）：1.1.0 下显式关闭 CommonSpecificDeclarations，行为与版本门反例一致——
// 每个 CJMP 修饰符只报版本门诊断，模式门 / 配对 / 族诊断零报告（门禁管行为：配对阶段早退）。
package cjmp_p

<!UNSUPPORTED_FEATURE!>public specific func b(): Int64 { 1 }<!>
''',
}
for name, text in files.items():
    open(base + name, 'w', encoding='utf-8', newline='\n').write(text)
print('ok')
