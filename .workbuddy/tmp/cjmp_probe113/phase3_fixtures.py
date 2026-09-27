R = 'D:/code/intellij/cangjie'
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt'


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


t = read(p)

# ---------- a) 修正 CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS（官方判据） ----------
old_block = '''        // 非 specific 抽象成员不能在 specific 类中
        if (!specificDecl.status.isAbstract) {
            for (member in specificDecl.declarations) {
                if (member !is CfirNamedFunction) continue
                if (member.status.isAbstract && !member.status.isSpecific) {
                    reporter.reportOn(
                        source = member.source,
                        factory = CfirErrors.CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS,
                        a = specificDecl.name,
                        b = "function",
                    )
                }
            }
        }'''
new_block = '''        // 官方判据（CheckCJMP.cpp:1310-1338 CheckAbstractClassMembers）：
        // specific **abstract class**（仅 CLASS_DECL）的成员不得为非 specific 的抽象成员；
        // 成员限 function/property，且跳过来自 common 部分的成员。
        if (specificDecl is CfirClass && specificDecl.status.isAbstract && specificDecl.status.isSpecific) {
            for (member in specificDecl.declarations) {
                val memberKind = when (member) {
                    is CfirNamedFunction -> "function"
                    is CfirProperty -> "property"
                    else -> continue
                }
                if (member.status.isAbstract && !member.status.isSpecific && !member.status.isCommon) {
                    reporter.reportOn(
                        source = member.source,
                        factory = CfirErrors.CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS,
                        a = specificDecl.name,
                        b = memberKind,
                    )
                }
            }
        }'''
t = replace_once(t, old_block, new_block, 'abstract member rule')

# ---------- b1) NOT_MATCHED 调用点参数形（specific 缺 common 方向） ----------
old_nm = '''                reporter.reportOn(
                    source = specificDecl.source,
                    factory = CfirErrors.NOT_MATCHED,
                    a = specificDecl.name,
                    b = declKindText(specificDecl),
                    c = "common",
                )'''
new_nm = '''                // 官方参数形（CheckCJMP.cpp:156 DiagNotMatchedDecl）：(side, "Kind 'name'", counterpart)
                reporter.reportOn(
                    source = specificDecl.source,
                    factory = CfirErrors.NOT_MATCHED,
                    a = "specific",
                    b = "${declKindText(specificDecl)} '${specificDecl.name.asString()}'",
                    c = "common",
                )'''
t = replace_once(t, old_nm, new_nm, 'not-matched args')

# ---------- b2) checkMemberMatch 的 NOT_MATCHED 调用点（common 成员缺 specific 方向） ----------
old_nm2 = '''                    reporter.reportOn(
                        source = commonMember.source ?: commonDecl.source,
                        factory = CfirErrors.NOT_MATCHED,
                        a = commonName,
                        b = commonKind,
                        c = "specific",
                    )'''
new_nm2 = '''                    // 官方 common 方向参数形（DiagNotMatchedCommonDecl）：("specific", "Kind 'name'", "common")
                    reporter.reportOn(
                        source = commonMember.source ?: commonDecl.source,
                        factory = CfirErrors.NOT_MATCHED,
                        a = "specific",
                        b = "$commonKind '${commonName.asString()}'",
                        c = "common",
                    )'''
t = replace_once(t, old_nm2, new_nm2, 'member not-matched args')

write(p, t)
print('checker patch applied (a+b)')

# ---------- c) 7 个 fixture 期望更新 ----------
import os
FX = R + '/cfir/analysis-tests/testData/diagnostics2/common-specific'


def patch_fixture(name, pairs):
    fp = os.path.join(FX, name)
    ft = read(fp)
    for old, new in pairs:
        if ft.count(old) == 1:
            ft = ft.replace(old, new, 1)
        else:
            nl2 = '\r\n' if '\r\n' in ft else '\n'
            old2, new2 = old.replace('\n', nl2), new.replace('\n', nl2)
            assert ft.count(old2) == 1, f"{name}: anchor {ft.count(old)}/{ft.count(old2)} for {old[:50]!r}"
            ft = ft.replace(old2, new2, 1)
    write(fp, ft)
    print(name + ' updated')


patch_fixture('commonSpecificOpenTypeMismatchPlaceholder.cj', [
    ('specific interface <!CLASSIFIER_REDECLARATION!>Service<!> {}',
     '<!NOT_MATCHED!>specific interface <!CLASSIFIER_REDECLARATION!>Service<!> {}<!>'),
])
patch_fixture('commonSpecificInterfaceKindMismatchPlaceholder.cj', [
    ('specific struct <!CLASSIFIER_REDECLARATION!>Config<!> {}',
     '<!NOT_MATCHED!>specific struct <!CLASSIFIER_REDECLARATION!>Config<!> {}<!>'),
])
patch_fixture('commonSpecificPairMismatchPlaceholder.cj', [
    ('''specific interface <!CLASSIFIER_REDECLARATION!>Printable<!> {
    <!PARSE_SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION!>func print(): Unit<!>
}''',
     '''<!NOT_MATCHED!>specific interface <!CLASSIFIER_REDECLARATION!>Printable<!> {
    <!PARSE_SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION!>func print(): Unit<!>
}<!>'''),
    ('specific class <!CLASSIFIER_REDECLARATION!>Printable<!> {}',
     '<!NOT_MATCHED!>specific class <!CLASSIFIER_REDECLARATION!>Printable<!> {}<!>'),
])
patch_fixture('commonSpecificClassKindMismatchPlaceholder.cj', [
    ('specific interface <!CLASSIFIER_REDECLARATION!>Repo<!> {}',
     '<!NOT_MATCHED!>specific interface <!CLASSIFIER_REDECLARATION!>Repo<!> {}<!>'),
])
patch_fixture('commonSpecificClassBoundaryPlaceholder.cj', [
    ('specific interface <!CLASSIFIER_REDECLARATION!>A<!> {}',
     '<!NOT_MATCHED!>specific interface <!CLASSIFIER_REDECLARATION!>A<!> {}<!>'),
])
patch_fixture('commonSpecificNotMatchedPlaceholder.cj', [
    ('specific class <!CLASSIFIER_REDECLARATION!>Printable<!> {}',
     '<!NOT_MATCHED!>specific class <!CLASSIFIER_REDECLARATION!>Printable<!> {}<!>'),
])
patch_fixture('commonSpecificParseRulesSpecific01.cj', [
    ('''specific interface I2 {
    <!PARSE_SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION!>func m(): Unit<!>
}''',
     '''<!NOT_MATCHED!>specific interface I2 {
    <!PARSE_SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION!>func m(): Unit<!>
}<!>'''),
])
print('all fixtures updated')
