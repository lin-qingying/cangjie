import os
import time

R = 'D:/code/intellij/cangjie'
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt'


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


t = read(p)

# 接口成员豁免（官方 MustMatchWithPlatform 第 5 条，CheckCJMP.cpp:822-825）
old = '''            if (commonName !in specificMemberNames) {
                // specific 中缺少 common 声明的成员——如果 common 成员有体则不需要 specific 实现
                val needsImpl = when (commonMember) {
                    is CfirNamedFunction -> commonMember.body == null
                    is CfirProperty -> commonMember.getter == null && commonMember.setter == null
                    else -> false
                }
                if (needsImpl) {'''
new = '''            if (commonName !in specificMemberNames) {
                // 接口成员豁免（官方 MustMatchWithPlatform 第 5 条，CheckCJMP.cpp:822-825）：
                // common 接口成员允许没有 specific 对应成员（接口成员可有默认实现或由 abstract 承载）。
                if (commonDecl is CfirInterface) continue
                // specific 中缺少 common 声明的成员——如果 common 成员有体（COMMON_WITH_DEFAULT 对位）
                // 则不需要 specific 实现；var 成员按官方调用点后置处理，不在此报告。
                val needsImpl = when (commonMember) {
                    is CfirNamedFunction -> commonMember.body == null
                    is CfirProperty -> commonMember.getter == null && commonMember.setter == null
                    else -> false
                }
                if (needsImpl) {'''
t = replace_once(t, old, new, 'interface exemption')

robust_write(p, t)
print('interface exemption applied')
