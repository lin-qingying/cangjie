import os
base = 'D:/code/intellij/cangjie/cfir/analysis-tests/testData/diagnostics2/common-specific/e2e/'
HEADER = '''// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7c.txt）
'''


def fixture(name, doc, common, specific):
    text = (HEADER + '// ' + doc + '\n'
            '// MODULE: common\n// CJMP_MODE: COMMON\n// FILE: common.cj\npackage cjmp_p\n\n' + common +
            '\n// MODULE: specific()()(common)\n// CJMP_MODE: SPECIFIC\n// FILE: specific.cj\npackage cjmp_p\n\n' + specific)
    open(base + name + '.cj', 'w', encoding='utf-8', newline='\n').write(text)


fixture('cjmpEnumConstructorMissing', 'common enum 构造器在 specific 缺失：common 方向 NOT_MATCHED（锚构造器，官方 common.cj:4:11）', '''public common enum E {
    | A | <!NOT_MATCHED!>B<!>
}
''', '''public specific enum E {
    | A
}
''')

fixture('cjmpEnumConstructorExtra', 'specific enum 多出构造器（common 穷尽）：specific 方向 NOT_MATCHED（锚构造器，官方 specific.cj:4:11）', '''public common enum E {
    | A
}
''', '''public specific enum E {
    | A | <!NOT_MATCHED!>B<!>
}
''')

fixture('cjmpEnumConstructorNonExhaustiveExtra', 'common 非穷尽 enum：specific 多出构造器合法（官方 COMMON_NON_EXHAUSTIVE 静默）', '''public common enum E {
    | A | ...
}
''', '''public specific enum E {
    | A | B | ...
}
''')

fixture('cjmpExtendOk', 'common extend 与 specific extend 按扩展类型配对，成员配对，零诊断', '''public common class C {
    public common init()
}
common extend C {
    public common func f(): Int64
}
''', '''public specific class C {
    public specific init() {}
}
specific extend C {
    public specific func f(): Int64 { 1 }
}
''')

fixture('cjmpExtendMissingMember', 'specific extend 缺成员：common 方向 NOT_MATCHED 锚 common extend 成员（官方 common.cj:7:5）', '''public common class C {
    public common init()
}
common extend C {
    <!NOT_MATCHED!>public common func f(): Int64<!>
}
''', '''public specific class C {
    public specific init() {}
}
specific extend C {
}
''')

fixture('cjmpGenericBoundStricter', 'specific 泛型约束严于 common：GENERIC_CONSTRAINT_NOT_LOOSER（锚 where 约束，官方 specific.cj:3:40）', '''public interface I {}
public common func g<T>(x: T): Int64
''', '''public specific func g<T>(x: T): Int64 <!GENERIC_CONSTRAINT_NOT_LOOSER!>where T <: I<!> { 1 }
''')

fixture('cjmpReturnCovariant', 'specific 返回类型为 common 返回类型的子型：配对成功（官方 IsFuncDeclSubType 返回协变）', '''public open class Base {}
public class Derived <: Base {}
public common func r(): Base
''', '''public specific func r(): Derived { Derived() }
''')
print('ok')
