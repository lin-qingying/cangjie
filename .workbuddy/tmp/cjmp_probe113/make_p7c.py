import os, shutil
base = 'D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/p7/'
shutil.rmtree(base, ignore_errors=True)
H = 'package cjmp_p\n\n'
cases = {
    'EnumCtorMissing': (H + '''public common enum E {
    | A | B
}
''', H + '''public specific enum E {
    | A
}
'''),
    'EnumCtorExtra': (H + '''public common enum E {
    | A
}
''', H + '''public specific enum E {
    | A | B
}
'''),
    'EnumCtorNonExhaustiveExtra': (H + '''public common enum E {
    | A | ...
}
''', H + '''public specific enum E {
    | A | B | ...
}
'''),
    'ExtendOk': (H + '''public common class C {
    public common init()
}
common extend C {
    public common func f(): Int64
}
''', H + '''public specific class C {
    public specific init() {}
}
specific extend C {
    public specific func f(): Int64 { 1 }
}
'''),
    'ExtendMissingMember': (H + '''public common class C {
    public common init()
}
common extend C {
    public common func f(): Int64
}
''', H + '''public specific class C {
    public specific init() {}
}
specific extend C {
}
'''),
    'GenericBoundStricter': (H + '''public interface I {}
public common func g<T>(x: T): Int64
''', H + '''public specific func g<T>(x: T): Int64 where T <: I { 1 }
'''),
    'ReturnCovariant': (H + '''public open class Base {}
public class Derived <: Base {}
public common func r(): Base
''', H + '''public specific func r(): Derived { Derived() }
'''),
}
for name, (c, s) in cases.items():
    d = base + name
    os.makedirs(d, exist_ok=True)
    open(d + '/common.cj', 'w', encoding='utf-8', newline='\n').write(c)
    open(d + '/specific.cj', 'w', encoding='utf-8', newline='\n').write(s)
print('ok')
