import os, time

R = 'D:/code/intellij/cangjie'
FX = R + '/cfir/analysis-tests/testData/diagnostics2/common-specific'


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


SPEC_NOTE = ('// CJMP_MODE: SPECIFIC\n'
             '// 本占位模型 specific part 编译（mode=SPECIFIC）：common 声明应来自 common part 的 cjo，\n'
             '// 源码内的 common 声明报 parse_common_in_non_common_file（计划 §8.1 矩阵）。\n')

edits = []

# 1) 混合占位：加模式指令 + common 声明包文件级 part 诊断
edits.append(('commonSpecificClassBoundaryPlaceholder.cj',
              '// RUN_PIPELINE_TILL: FRONTEND\n',
              SPEC_NOTE + '// RUN_PIPELINE_TILL: FRONTEND\n'))
edits.append(('commonSpecificClassBoundaryPlaceholder.cj',
              'common open class <!PARSE_CJMP_IN_COMMON_CTOR_REQUIRED!>A<!> {}',
              '<!PARSE_COMMON_IN_NON_COMMON_FILE!>common open class <!PARSE_CJMP_IN_COMMON_CTOR_REQUIRED!>A<!> {}<!>'))

edits.append(('commonSpecificClassKindMismatchPlaceholder.cj',
              '// RUN_PIPELINE_TILL: FRONTEND\n',
              SPEC_NOTE + '// RUN_PIPELINE_TILL: FRONTEND\n'))
edits.append(('commonSpecificClassKindMismatchPlaceholder.cj',
              'common class <!PARSE_CJMP_IN_COMMON_CTOR_REQUIRED!>Repo<!> {}',
              '<!PARSE_COMMON_IN_NON_COMMON_FILE!>common class <!PARSE_CJMP_IN_COMMON_CTOR_REQUIRED!>Repo<!> {}<!>'))

edits.append(('commonSpecificInterfaceKindMismatchPlaceholder.cj',
              '// RUN_PIPELINE_TILL: FRONTEND\n',
              SPEC_NOTE + '// RUN_PIPELINE_TILL: FRONTEND\n'))
edits.append(('commonSpecificInterfaceKindMismatchPlaceholder.cj',
              'common interface Config {\n    func ready(): Bool\n}',
              '<!PARSE_COMMON_IN_NON_COMMON_FILE!>common interface Config {\n    func ready(): Bool\n}<!>'))

edits.append(('commonSpecificNotMatchedPlaceholder.cj',
              '// RUN_PIPELINE_TILL: FRONTEND\n',
              SPEC_NOTE + '// RUN_PIPELINE_TILL: FRONTEND\n'))
edits.append(('commonSpecificNotMatchedPlaceholder.cj',
              'common interface Printable {\n    func print(): Unit\n}',
              '<!PARSE_COMMON_IN_NON_COMMON_FILE!>common interface Printable {\n    func print(): Unit\n}<!>'))

edits.append(('commonSpecificOpenTypeMismatchPlaceholder.cj',
              '// RUN_PIPELINE_TILL: FRONTEND\n',
              SPEC_NOTE + '// RUN_PIPELINE_TILL: FRONTEND\n'))
edits.append(('commonSpecificOpenTypeMismatchPlaceholder.cj',
              'common open class <!PARSE_CJMP_IN_COMMON_CTOR_REQUIRED!>Service<!> {}',
              '<!PARSE_COMMON_IN_NON_COMMON_FILE!>common open class <!PARSE_CJMP_IN_COMMON_CTOR_REQUIRED!>Service<!> {}<!>'))

edits.append(('commonSpecificPairMismatchPlaceholder.cj',
              '// RUN_PIPELINE_TILL: FRONTEND\n',
              SPEC_NOTE + '// RUN_PIPELINE_TILL: FRONTEND\n'))
edits.append(('commonSpecificPairMismatchPlaceholder.cj',
              'common interface Printable {\n    func print(): Unit\n}',
              '<!PARSE_COMMON_IN_NON_COMMON_FILE!>common interface Printable {\n    func print(): Unit\n}<!>'))

# 2) 纯 common 占位：模式 = COMMON
edits.append(('commonSpecificOpenClassNoInitPlaceholder.cj',
              '// RUN_PIPELINE_TILL: FRONTEND\n',
              '// CJMP_MODE: COMMON\n// RUN_PIPELINE_TILL: FRONTEND\n'))

# 3) parse 族正例 fixture：按侧别补模式
edits.append(('commonSpecificParseRulesCommon01.cj',
              '// LANGUAGE_VERSION: 1.1.0\n',
              '// LANGUAGE_VERSION: 1.1.0\n// CJMP_MODE: COMMON\n'))
edits.append(('commonSpecificParseRulesSpecific01.cj',
              '// LANGUAGE_VERSION: 1.1.0\n',
              '// LANGUAGE_VERSION: 1.1.0\n// CJMP_MODE: SPECIFIC\n'))

for name, old, new in edits:
    fp = os.path.join(FX, name)
    t = read(fp)
    t = replace_once(t, old, new, name)
    robust_write(fp, t)
    print('patched', name, '::', old.splitlines()[0][:50])

# 4) LLT 04_diff_file_extend：common 声明需要 common part 模式，否则触发模式门
for name in ('1.cj', '2.cj'):
    fp = R + '/cfir/analysis-tests/testData/llt/redefinition/private_func/04_diff_file_extend/' + name
    t = read(fp)
    if 'CJMP_MODE' not in t:
        robust_write(fp, '// CJMP_MODE: COMMON\n' + t)
        print('patched llt', name)
    else:
        print('llt', name, 'already has directive')

print('fixture patch complete')
