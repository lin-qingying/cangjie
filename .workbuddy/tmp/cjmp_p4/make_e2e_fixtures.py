import os
# 由 cjc 1.1.3 探针（.workbuddy/tmp/cjmp_probe113/probe_results7.txt）映射而来的多模块 e2e fixture
base = 'D:/code/intellij/cangjie/cfir/analysis-tests/testData/diagnostics2/common-specific/e2e/'
os.makedirs(base, exist_ok=True)

HEADER = '''// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
'''


def fixture(name, doc, common, specific):
    text = (HEADER + '// ' + doc + '\n'
            '// MODULE: common\n// CJMP_MODE: COMMON\n// FILE: common.cj\npackage cjmp_p\n\n' + common +
            '\n// MODULE: specific()()(common)\n// CJMP_MODE: SPECIFIC\n// FILE: specific.cj\npackage cjmp_p\n\n' + specific)
    open(base + name + '.cj', 'w', encoding='utf-8', newline='\n').write(text)


fixture('cjmpOkBasic', '正例：class/struct/顶层函数/enum 全部配对，零诊断', '''public common class Repo {
    public common init()
    public common func get(): Int64
}

public common struct Point {
    public common init()
}

public common func platform(): String

public common enum Color {
    | Red | Green
}
''', '''public specific class Repo {
    public specific init() {}
    public specific func get(): Int64 { 1 }
}

public specific struct Point {
    public specific init() {}
}

public specific func platform(): String {
    "spec"
}

public specific enum Color {
    | Red | Green
}
''')

fixture('cjmpMissingSpecific', 'common 声明缺 specific 实现：common 方向 NOT_MATCHED（锚 common 声明）', '''public common func a(): Int64
<!NOT_MATCHED!>public common func b(): Int64<!>
''', '''public specific func a(): Int64 { 1 }
''')

fixture('cjmpOrphanSpecific', 'specific 声明无 common 对应：specific 方向 NOT_MATCHED', '''public common func a(): Int64
''', '''public specific func a(): Int64 { 1 }
<!NOT_MATCHED!>public specific func z(): Int64 { 2 }<!>
''')

fixture('cjmpReturnTypeMismatch', '返回类型不同：1.1.3 按不配对处理（双向 NOT_MATCHED），不报返回类型诊断', '''<!NOT_MATCHED!>public common func a(): Int64<!>
''', '''<!NOT_MATCHED!>public specific func a(): String { "" }<!>
''')

fixture('cjmpParamTypeMismatch', '参数类型不同：不配对（双向 NOT_MATCHED）', '''<!NOT_MATCHED!>public common func a(x: Int64): Int64<!>
''', '''<!NOT_MATCHED!>public specific func a(x: String): Int64 { 1 }<!>
''')

fixture('cjmpNamedParamName', '命名参数名不同：参数诊断 + 双向 NOT_MATCHED', '''<!NOT_MATCHED!>public common func a(x!: Int64): Int64<!>
''', '''<!NOT_MATCHED!>public specific func a(<!SPECIFIC_HAS_DIFFERENT_PARAMETER!>y!: Int64<!>): Int64 { 1 }<!>
''')

fixture('cjmpCommonDefaultOmitted', 'common 自带默认实现时 specific 可省略（MustMatchWithPlatform 豁免）', '''public common func a(): Int64 {
    1
}
public common func b(): Int64
''', '''public specific func b(): Int64 { 2 }
''')

fixture('cjmpKindMismatch', 'class 与 struct 种类不同：种类诊断 + 成员与声明双向 NOT_MATCHED', '''<!NOT_MATCHED!>public common class K {
    <!NOT_MATCHED!>public common init()<!>
}<!>
''', '''<!SPECIFIC_HAS_DIFFERENT_KIND!>public specific struct K {
    <!NOT_MATCHED!>public specific init() {}<!>
}<!>
''')

fixture('cjmpMemberMissing', 'common 类成员缺 specific 实现：成员级 NOT_MATCHED（锚 common 成员）', '''public common class R {
    public common init()
    <!NOT_MATCHED!>public common func f(): Int64<!>
}
''', '''public specific class R {
    public specific init() {}
}
''')

fixture('cjmpEnumNonExhaustive', 'common 穷尽 enum 与 specific 非穷尽 enum 不兼容', '''public common enum E {
    | A | B
}
''', '''<!COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH!>public specific enum E {
    | A | B | ...
}<!>
''')

fixture('cjmpVisibilityMismatch', '可见性不同：修饰符诊断（仍配对，无 NOT_MATCHED）', '''public common func a(): Int64
''', '''<!SPECIFIC_HAS_DIFFERENT_MODIFIER!>specific func a(): Int64 { 1 }<!>
''')

fixture('cjmpDefaultBothSides', '默认值双侧：默认值诊断 + 双向 NOT_MATCHED', '''<!NOT_MATCHED!>public common func a(x!: Int64 = 1): Int64<!>
''', '''<!NOT_MATCHED!>public specific func a(<!CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES!>x!: Int64 = 2<!>): Int64 { x }<!>
''')

fixture('cjmpDefaultReadThrough', 'common 侧默认值对 specific 调用读穿（D7），调用 a() 合法', '''public common func a(x!: Int64 = 1): Int64

public func use(): Int64 {
    a()
}
''', '''public specific func a(x!: Int64): Int64 { x }

public func use2(): Int64 {
    a()
}
''')

fixture('cjmpLetTypeMismatch', 'let 类型不同：类型诊断（仍配对）', '''public common let v: Int64
''', '''<!SPECIFIC_HAS_DIFFERENT_TYPE!>public specific let v: String = ""<!>
''')

fixture('cjmpInterfaceMemberExempt', 'common 接口成员可无 specific 对应成员（MustMatchWithPlatform 接口豁免）', '''public common interface I {
    func m(): Int64
}
''', '''public specific interface I {
}
''')
print('ok')
