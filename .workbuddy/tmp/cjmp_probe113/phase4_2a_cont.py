import os, re, time

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


def insert_after_copied_pair(text, rel_new_line, what):
    """在 `copied.isSpecific = status.isSpecific` 之后按同缩进插入新拷贝行。"""
    pattern = re.compile(r'([ \t]*)copied\.isSpecific = status\.isSpecific')
    matches = pattern.findall(text)
    assert len(matches) == 1, f"{what}: found {len(matches)} copies"
    indent = matches[0]
    old = f'{indent}copied.isSpecific = status.isSpecific'
    new = old + '\n' + indent + rel_new_line
    return text.replace(old, new, 1)


# 4b) LightTree 拷贝
p = R + '/cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt'
t = read(p)
if 'isCommonWithDefault' not in t:
    t = insert_after_copied_pair(t, 'copied.isCommonWithDefault = status.isCommonWithDefault', 'llt copy')
    robust_write(p, t)
    print('4b llt copy')
else:
    print('4b llt copy already present')

# 5) 反序列化：去重 + 读位
p = R + '/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/deserialize/CfirDeclDeserializer.kt'
t = read(p)
t = replace_once(
    t,
    '''        status.isCommon = testAttr(decl, AttrBit.COMMON) || testAttr(decl, AttrBit.FROM_COMMON_PART)
        status.isSpecific = testAttr(decl, AttrBit.SPECIFIC)
        status.isCommon = testAttr(decl, AttrBit.COMMON) || testAttr(decl, AttrBit.FROM_COMMON_PART)
        status.isSpecific = testAttr(decl, AttrBit.SPECIFIC)''',
    '''        status.isCommon = testAttr(decl, AttrBit.COMMON) || testAttr(decl, AttrBit.FROM_COMMON_PART)
        status.isSpecific = testAttr(decl, AttrBit.SPECIFIC)
        // 官方 ASTLoader 原样恢复 AttributePack；COMMON_WITH_DEFAULT 决定跨模块验证豁免
        //（CheckCJMP.cpp MustMatchWithPlatform），必须随位恢复而不是从函数体推导——
        // 反序列化声明没有函数体。
        status.isCommonWithDefault = testAttr(decl, AttrBit.COMMON_WITH_DEFAULT)''',
    'deser buildStatus')
t = insert_after_copied_pair(t, 'copied.isCommonWithDefault = status.isCommonWithDefault', 'deser cloneStatus')
robust_write(p, t)
print('5 deserializer')

# 6) 写侧：CJMP 属性位
p = R + '/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/CfirCjoPackageMetadataProducer.kt'
t = read(p)
t = replace_once(
    t,
    '''            if (status.isUnsafe) values += CfirAttribute.UNSAFE
            if (status.isMut) values += CfirAttribute.MUT''',
    '''            if (status.isUnsafe) values += CfirAttribute.UNSAFE
            if (status.isMut) values += CfirAttribute.MUT
            addCjmpAttributes(values, declaration, status)''',
    'writer hook')
t = replace_once(
    t,
    '''        private fun attributes(declaration: CfirDeclaration, topLevel: Boolean): List<ULong> {''',
    '''        /**
         * 写出 CJMP 属性位（官方 `Attribute::COMMON / SPECIFIC / FROM_COMMON_PART /
         * COMMON_WITH_DEFAULT`）。
         *
         * 证据（cjc 1.1.3 产出的 cjo 实测，`.workbuddy/tmp/cjmp_probe113/full/*.cjo`）：
         * - `specific func` → 只带 `SPECIFIC`；
         * - `common func`（有体）→ `COMMON + FROM_COMMON_PART + COMMON_WITH_DEFAULT`。
         *
         * `COMMON_WITH_DEFAULT` 按官方 `SetCJMPAttrs`（ParseCJMPDecl.cpp:57-65 + :141-161）
         * 在**写侧派生**：common 声明自带默认实现（函数有体 / property 有访问器 / var 有初始值）
         * 即置位。`FROM_COMMON_PART` 标记"该声明来自 common part 的 cjo"——本仓库当前只写出
         * 源声明（[collect] 要求 `origin == Source`），故该位仅对反序列化来源的声明防御性保留。
         */
        private fun addCjmpAttributes(
            values: MutableSet<CfirAttribute>,
            declaration: CfirDeclaration,
            status: org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus,
        ) {
            if (status.isSpecific) values += CfirAttribute.SPECIFIC
            if (!status.isCommon) return
            values += CfirAttribute.COMMON
            if (declaration.origin != org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin.Source) {
                values += CfirAttribute.FROM_COMMON_PART
            }
            if (status.isCommonWithDefault || hasCjmpDefault(declaration)) {
                values += CfirAttribute.COMMON_WITH_DEFAULT
            }
        }

        /** 官方 `HasDefault` 对位：声明自身是否自带默认实现/初始化。 */
        private fun hasCjmpDefault(declaration: CfirDeclaration): Boolean = when (declaration) {
            is org.cangnova.cangjie.cfir.declarations.CfirNamedFunction -> declaration.body != null
            is org.cangnova.cangjie.cfir.declarations.CfirProperty ->
                declaration.getter != null || declaration.setter != null

            is org.cangnova.cangjie.cfir.declarations.CfirVariable -> declaration.initializer != null
            else -> false
        }

        private fun attributes(declaration: CfirDeclaration, topLevel: Boolean): List<ULong> {''',
    'writer helper')
robust_write(p, t)
print('6 writer cjmp bits')
print('phase4_2a-cont complete')
