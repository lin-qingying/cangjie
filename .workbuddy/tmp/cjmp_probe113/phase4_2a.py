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


# ---------------------------------------------------------------- 1) impl: 新位
p = R + '/cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/impl/CfirDeclarationStatusImpl.kt'
t = read(p)
t = replace_once(
    t,
    '''    /**
     * 声明是否带有 specific 修饰。
     */
    override var isSpecific: Boolean
        get() = this[Modifier.SPECIFIC]
        set(value) {
            this[Modifier.SPECIFIC] = value
        }
''',
    '''    /**
     * 声明是否带有 specific 修饰。
     */
    override var isSpecific: Boolean
        get() = this[Modifier.SPECIFIC]
        set(value) {
            this[Modifier.SPECIFIC] = value
        }

    /**
     * common 声明是否自带默认实现/初始化（官方 `Attribute::COMMON_WITH_DEFAULT`）。
     *
     * 官方在解析期由 `SetCJMPAttrs` 派生（函数有体 / property 有访问器 / var 有初始值；
     * 类样式声明在全部 common 成员都有默认实现时同样置位）。本仓库在写侧序列化时派生、
     * 读侧原样恢复：跨模块（common cjo → specific 编译）验证豁免必须依赖该位，
     * 因为反序列化声明的函数体不随 cjo 落盘。
     */
    override var isCommonWithDefault: Boolean
        get() = this[Modifier.COMMON_WITH_DEFAULT]
        set(value) {
            this[Modifier.COMMON_WITH_DEFAULT] = value
        }
''',
    'impl property')
t = replace_once(
    t,
    '''        /** 解析后的 C ABI 标记。 */
        C(0x20000),
    }''',
    '''        /** 解析后的 C ABI 标记。 */
        C(0x20000),
        /** 官方 `Attribute::COMMON_WITH_DEFAULT` 位（写侧派生、读侧恢复）。 */
        COMMON_WITH_DEFAULT(0x40000),
    }''',
    'impl modifier bit')
robust_write(p, t)
print('1 impl bit + property')

# ---------------------------------------------------------------- 2) status defaults
p = R + '/cfir/cfir-tree/src/org/cangnova/cangjie/cfir/declarations/CfirResolvedDeclarationStatusDefaults.kt'
t = read(p)
t = replace_once(
    t,
    '''        isCommon = false
        isSpecific = false
        isRedef = false''',
    '''        isCommon = false
        isSpecific = false
        isCommonWithDefault = false
        isRedef = false''',
    'defaults DEFAULT_STATUS')
t = replace_once(
    t,
    '''        isCommon = this@resolvedForStatuslessDeclaration.isCommon
        isSpecific = this@resolvedForStatuslessDeclaration.isSpecific
        isRedef = this@resolvedForStatuslessDeclaration.isRedef''',
    '''        isCommon = this@resolvedForStatuslessDeclaration.isCommon
        isSpecific = this@resolvedForStatuslessDeclaration.isSpecific
        isCommonWithDefault = this@resolvedForStatuslessDeclaration.isCommonWithDefault
        isRedef = this@resolvedForStatuslessDeclaration.isRedef''',
    'defaults resolvedForStatusless')
robust_write(p, t)
print('2 status defaults')

# ---------------------------------------------------------------- 3) resolve 状态重建
p = R + '/cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirStatusResolveProcessor.kt'
t = read(p)
t = replace_once(
    t,
    '''            isCommon = currentStatus.isCommon
            isSpecific = currentStatus.isSpecific
            isRedef = currentStatus.isRedef''',
    '''            isCommon = currentStatus.isCommon
            isSpecific = currentStatus.isSpecific
            isCommonWithDefault = currentStatus.isCommonWithDefault
            isRedef = currentStatus.isRedef''',
    'resolve site A')
t = replace_once(
    t,
    '''            isCommon = status.isCommon
            isSpecific = status.isSpecific
            isRedef = status.isRedef''',
    '''            isCommon = status.isCommon
            isSpecific = status.isSpecific
            isCommonWithDefault = status.isCommonWithDefault
            isRedef = status.isRedef''',
    'resolve site B')
robust_write(p, t)
print('3 resolve status processor')

# ---------------------------------------------------------------- 4) raw 拷贝（PSI / LightTree）
for rel in ['/cfir/raw-cfir/psi2cfir/src/org/cangnova/cangjie/cfir/builder/PsiRawCfirBuilder.kt',
            '/cfir/raw-cfir/light-tree2cfir/src/org/cangnova/cangjie/cfir/lightTree/LightTreeRawCfirDeclarationBuilder.kt']:
    p = R + rel
    t = read(p)
    t = replace_once(
        t,
        '''                copied.isCommon = status.isCommon
                copied.isSpecific = status.isSpecific''',
        '''                copied.isCommon = status.isCommon
                copied.isSpecific = status.isSpecific
                copied.isCommonWithDefault = status.isCommonWithDefault''',
        'raw copy ' + os.path.basename(rel))
    robust_write(p, t)
    print('4 raw copy', os.path.basename(rel))

# ---------------------------------------------------------------- 5) 反序列化：去重 + 读位 + 拷贝
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
        // （CheckCJMP.cpp MustMatchWithPlatform），必须随位恢复而不是从函数体推导——
        // 反序列化声明没有函数体。
        status.isCommonWithDefault = testAttr(decl, AttrBit.COMMON_WITH_DEFAULT)''',
    'deser buildStatus')
t = replace_once(
    t,
    '''            copied.isCommon = status.isCommon
            copied.isSpecific = status.isSpecific''',
    '''            copied.isCommon = status.isCommon
            copied.isSpecific = status.isSpecific
            copied.isCommonWithDefault = status.isCommonWithDefault''',
    'deser cloneStatus')
robust_write(p, t)
print('5 deserializer')

# ---------------------------------------------------------------- 6) 写侧：CJMP 属性位
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
         * 即置位；类样式声明的全部 common 成员都有默认实现时父声明同样置位。
         * `FROM_COMMON_PART` 标记"该声明来自 common part 的 cjo"——本仓库当前只写出源声明
         * （[collect] 要求 `origin == Source`），故该位仅对反序列化来源的声明防御性保留。
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

print('phase4_2a complete')
