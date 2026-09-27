import os
base = 'D:/code/intellij/cangjie/cfir/analysis-tests/testData/diagnostics2/common-specific/e2e/'
HEADER = '''// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7b.txt）；替换原单文件占位 fixture
'''


def fixture(name, doc, common, specific):
    text = (HEADER + '// ' + doc + '\n'
            '// MODULE: common\n// CJMP_MODE: COMMON\n// FILE: common.cj\npackage cjmp_p\n\n' + common +
            '\n// MODULE: specific()()(common)\n// CJMP_MODE: SPECIFIC\n// FILE: specific.cj\npackage cjmp_p\n\n' + specific)
    open(base + name + '.cj', 'w', encoding='utf-8', newline='\n').write(text)


fixture('cjmpKindMismatchClassInterface', 'common class 与 specific interface：种类诊断 + common 声明与其 common 成员双向 NOT_MATCHED', '''<!NOT_MATCHED!>public common class Repo {
    <!NOT_MATCHED!>public common init()<!>
}<!>
''', '''<!SPECIFIC_HAS_DIFFERENT_KIND!>public specific interface Repo {}<!>
''')

fixture('cjmpKindMismatchOpenClassInterface', 'common open class 与 specific interface：同上（open 不影响种类判定）', '''<!NOT_MATCHED!>public common open class Service {
    <!NOT_MATCHED!>public common init()<!>
}<!>
''', '''<!SPECIFIC_HAS_DIFFERENT_KIND!>public specific interface Service {}<!>
''')

fixture('cjmpKindMismatchInterfaceStruct', 'common interface 与 specific struct：只报种类诊断——interface 成员未标 common，接口整体视为自带默认实现（COMMON_WITH_DEFAULT），不要求配对', '''public common interface Config {
    func ready(): Bool
}
''', '''<!SPECIFIC_HAS_DIFFERENT_KIND!>public specific struct Config {}<!>
''')

fixture('cjmpKindMismatchInterfaceClass', 'common interface 与 specific class：同上', '''public common interface Printable {
    func print(): Unit
}
''', '''<!SPECIFIC_HAS_DIFFERENT_KIND!>public specific class Printable {}<!>
''')

fixture('cjmpDuplicateSpecific', '同一 common 被两个 specific 绑定：重载冲突 + common 上 MULTIPLE_COMMON_IMPLEMENTATIONS + 第二个 specific NOT_MATCHED', '''<!MULTIPLE_COMMON_IMPLEMENTATIONS!>public common func f(): Int64<!>
''', '''public specific func f(): Int64 { 1 }
<!NOT_MATCHED!>public specific func f(): Int64 { 2 }<!>
''')
print('ok')
