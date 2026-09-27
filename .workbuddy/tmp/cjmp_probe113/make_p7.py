import os
base = 'D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/p7/'
H = 'package cjmp_p\n\n'
cases = {
    # 正例：class/struct/func/var/enum 全部配对
    'ok_basic': (
        H + '''public common class Repo {
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
''',
        H + '''public specific class Repo {
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
'''),
    # common 缺 specific（common 方向 NOT_MATCHED）
    'missing_specific': (
        H + '''public common func a(): Int64
public common func b(): Int64
''',
        H + '''public specific func a(): Int64 { 1 }
'''),
    # specific 无 common（specific 方向 NOT_MATCHED）
    'orphan_specific': (
        H + '''public common func a(): Int64
''',
        H + '''public specific func a(): Int64 { 1 }
public specific func z(): Int64 { 2 }
'''),
    # 返回类型不匹配
    'return_type_mismatch': (
        H + '''public common func a(): Int64
''',
        H + '''public specific func a(): String { "" }
'''),
    # 参数类型不同 = 不配对
    'param_type_mismatch': (
        H + '''public common func a(x: Int64): Int64
''',
        H + '''public specific func a(x: String): Int64 { 1 }
'''),
    # 命名参数名不同
    'named_param_name': (
        H + '''public common func a(x!: Int64): Int64
''',
        H + '''public specific func a(y!: Int64): Int64 { 1 }
'''),
    # common 带默认实现，specific 省略
    'common_default_omitted': (
        H + '''public common func a(): Int64 {
    1
}
public common func b(): Int64
''',
        H + '''public specific func b(): Int64 { 2 }
'''),
    # 种类不同（common class / specific struct）
    'kind_mismatch': (
        H + '''public common class K {
    public common init()
}
''',
        H + '''public specific struct K {
    public specific init() {}
}
'''),
    # 类成员缺 specific
    'member_missing': (
        H + '''public common class R {
    public common init()
    public common func f(): Int64
}
''',
        H + '''public specific class R {
    public specific init() {}
}
'''),
    # enum 穷尽性
    'enum_nonexhaustive': (
        H + '''public common enum E {
    | A | B
}
''',
        H + '''public specific enum E {
    | A | B | ...
}
'''),
    # 可见性不同
    'visibility_mismatch': (
        H + '''public common func a(): Int64
''',
        H + '''specific func a(): Int64 { 1 }
'''),
    # 默认值双侧
    'default_both_sides': (
        H + '''public common func a(x!: Int64 = 1): Int64
''',
        H + '''public specific func a(x!: Int64 = 2): Int64 { x }
'''),
    # specific 使用 common 默认值（读穿）
    'default_read_through': (
        H + '''public common func a(x!: Int64 = 1): Int64

public func use(): Int64 {
    a()
}
''',
        H + '''public specific func a(x!: Int64): Int64 { x }

public func use2(): Int64 {
    a()
}
'''),
    # var 类型不同
    'var_type_mismatch': (
        H + '''public common let v: Int64
''',
        H + '''public specific let v: String = ""
'''),
    # interface 成员豁免
    'iface_exempt': (
        H + '''public common interface I {
    func m(): Int64
}
''',
        H + '''public specific interface I {
}
'''),
}
for name, (c, s) in cases.items():
    d = base + name
    os.makedirs(d, exist_ok=True)
    open(d + '/common.cj', 'w', encoding='utf-8', newline='\n').write(c)
    open(d + '/specific.cj', 'w', encoding='utf-8', newline='\n').write(s)
print('ok', len(cases))
