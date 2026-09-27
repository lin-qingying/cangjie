import os, time

R = 'D:/code/intellij/cangjie'


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


# 检查器消费 COMMON_WITH_DEFAULT：common 成员自带默认实现时豁免"specific 必须实现"判定
p = R + '/cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt'
t = read(p)
t = replace_once(
    t,
    '''                val needsImpl = when (commonMember) {
                    is CfirNamedFunction -> commonMember.body == null
                    is CfirProperty -> commonMember.getter == null && commonMember.setter == null
                    else -> false
                }''',
    '''                // 官方 `MustMatchWithPlatform` 第 1 条豁免（CheckCJMP.cpp:811-818）：
                // common 声明带 COMMON_WITH_DEFAULT（自带默认实现/初始化）时不需要 specific 实现。
                // 该位在写侧派生、读侧恢复——反序列化声明没有函数体，不能靠 body 判据。
                val hasCommonWithDefault =
                    (commonMember as? CfirMemberDeclaration)?.status?.isCommonWithDefault == true
                val needsImpl = !hasCommonWithDefault && when (commonMember) {
                    is CfirNamedFunction -> commonMember.body == null
                    is CfirProperty -> commonMember.getter == null && commonMember.setter == null
                    else -> false
                }''',
    'checker commonWithDefault exemption')
robust_write(p, t)
print('checker exemption applied')
