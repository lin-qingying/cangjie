R = 'D:/code/intellij/cangjie/cfir/analysis-tests/testData/diagnostics/coverage/declaration-status'


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


old = '    static <!WRONG_MODIFIER_TARGET!>abstract<!> func absFn(): Unit'
new = '    <!PARSE_EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS!>static <!WRONG_MODIFIER_TARGET!>abstract<!> func absFn(): Unit<!>'

for f in ['memberStatusCheckersRich.cj', 'staticIncompatibleModifiersRich.cj']:
    p = R + '/' + f
    t = read(p)
    if 'PARSE_EXPLICITLY_ABSTRACT_ONLY_FOR_CJMP_ABSTRACT_CLASS' in t:
        print(f + ': already updated')
        continue
    t = replace_once(t, old, new, f)
    write(p, t)
    print(f + ': updated')
