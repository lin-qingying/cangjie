import os, shutil
base = 'D:/code/intellij/cangjie/.workbuddy/tmp/cjmp_probe113/p7/'
shutil.rmtree(base, ignore_errors=True)
H = 'package cjmp_p\n\n'
cases = {
    'OpenTypeMismatch': (H + '''public common open class Service {
    public common init()
}
''', H + '''public specific interface Service {}
'''),
    'InterfaceKindMismatch': (H + '''public common interface Config {
    func ready(): Bool
}
''', H + '''public specific struct Config {}
'''),
    'PairMismatch': (H + '''public common interface Printable {
    func print(): Unit
}
''', H + '''public specific interface Printable {
    func print(): Unit
}

public specific class Printable {}
'''),
    'ClassKindMismatch': (H + '''public common class Repo {
    public common init()
}
''', H + '''public specific interface Repo {}
'''),
    'NotMatched': (H + '''public common interface Printable {
    func print(): Unit
}
''', H + '''public specific class Printable {}
'''),
    'DuplicateSpecific': (H + '''public common func f(): Int64
''', H + '''public specific func f(): Int64 { 1 }
public specific func f(): Int64 { 2 }
'''),
}
for name, (c, s) in cases.items():
    d = base + name
    os.makedirs(d, exist_ok=True)
    open(d + '/common.cj', 'w', encoding='utf-8', newline='\n').write(c)
    open(d + '/specific.cj', 'w', encoding='utf-8', newline='\n').write(s)
print('ok')
