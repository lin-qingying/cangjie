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

# ---------- 0) 导入 ----------
add_imports = [
    'import org.cangnova.cangjie.cfir.resolve.cjmp.cjmpMappingStorageOrNull',
    'import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor',
    'import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias',
]
anchor_imp = 'import org.cangnova.cangjie.cfir.declarations.CfirExtendBase'
if anchor_imp not in t:
    anchor_imp = 'import org.cangnova.cangjie.cfir.declarations.CfirDeclaration'
assert anchor_imp in t, 'import anchor missing'
for imp in add_imports:
    if imp + '\n' not in t:
        t = replace_once(t, anchor_imp, imp + '\n' + anchor_imp, 'import ' + imp.split('.')[-1])

# ---------- 1) check() 重构 ----------
old_check = '''    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass) return

        // common/specific 声明族是官方 v1.1.0 才引入的语言表面（v1.0.x cjc 解析期
        // 即报 parse_expected_decl，CJMP 语义检查根本不存在），1.0.x 下整族跳过。
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations)) return

        // 只对 specific 声明执行匹配检查
        if (declaration.status.isSpecific) {
            checkSpecificMatchesCommon(declaration)
            checkSpecificExtraConstraints(declaration)
        }

        // 对 common 声明执行约束检查
        if (declaration.status.isCommon) {
            checkCommonDeclarationConstraints(declaration)
            checkCommonExtraConstraints(declaration)
        }

        // common/specific 声明的修饰符和注解限制
        if (declaration.status.isCommon || declaration.status.isSpecific) {
            checkCommonSpecificAnnotations(declaration)
            checkCJMPAbstractClassMembers(declaration)
            checkExplicitlyAbstractUsage(declaration)
        }
    }'''
new_check = '''    override fun check(declaration: CfirClassLikeDeclaration) {
        // 覆盖面（G21）：class/struct/interface/enum 全量参与（官方 MatchNominativeDecl 面）；
        // typealias 不参与 CJMP 配对（修饰符谓词已拒绝其携带 cjmp）。
        if (declaration is CfirTypeAlias) return

        // 版本门（D16 单入口）。
        if (!CjmpGate.isEnabled(context)) return

        // 只对 specific 声明执行匹配检查
        if (declaration.status.isSpecific) {
            checkSpecificMatchesCommon(declaration)
            checkSpecificExtraConstraints(declaration)
        }

        // 对 common 声明执行约束检查
        if (declaration.status.isCommon) {
            checkCommonDeclarationConstraints(declaration)
            checkCommonExtraConstraints(declaration)
        }

        // common/specific 声明的修饰符和注解限制
        if (declaration.status.isCommon || declaration.status.isSpecific) {
            checkCommonSpecificAnnotations(declaration)
            checkCJMPAbstractClassMembers(declaration)
            // 注意：explicitly abstract 的 sema 诊断无官方触发点（origin/main 全文无引用），
            // 该形态由 parse 变体统一负责（CfirCjmpParseRulesChecker，1.1.3 实测消息对位），
            // 此处不再重复检查（Phase 3 收敛：sema 条目已登记为后续清理项）。
        }
    }'''
t = replace_once(t, old_check, new_check, 'check()')

# ---------- 2) checkSpecificMatchesCommon ----------
old_head = '''    private fun checkSpecificMatchesCommon(specificDecl: CfirClass) {
        // 通过 classId 在 common 模块中查找同名声明
        val classId = specificDecl.symbol.classId
        val commonSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
        val commonDecl = commonSymbol?.cfir as? CfirClass

        if (commonDecl == null || !commonDecl.status.isCommon) {
            // specific 声明找不到匹配的 common 声明
            // 注意：这不一定是错误——specific 可以有自己独有的声明
            return
        }
'''
new_head = '''    private fun checkSpecificMatchesCommon(specificDecl: CfirClassLikeDeclaration) {
        // 配对结果一律消费匹配期存储（C17/G3）：不再 symbolProvider 现查。
        val storage = context.session.cjmpMappingStorageOrNull
        val commonDecl = storage?.commonFor(specificDecl) as? CfirClassLikeDeclaration
        if (commonDecl == null || !commonDecl.status.isCommon) {
            // 匹配期消费：确有无候选或结构性失配记录时才报 NOT_MATCHED；
            // specific 独有声明（无 cjmp 候选）保持静默。
            if (storage != null &&
                (storage.isUnmatched(specificDecl) || storage.mismatchKindsFor(specificDecl).isNotEmpty())
            ) {
                reporter.reportOn(
                    source = specificDecl.source,
                    factory = CfirErrors.NOT_MATCHED,
                    a = specificDecl.name,
                    b = declKindText(specificDecl),
                    c = "common",
                )
            }
            return
        }

        // MULTIPLE_COMMON_IMPLEMENTATIONS（C17：matcher 第二绑定事件 → checker 消费存储报诊断）。
        val bindings = storage?.specificBindingsFor(commonDecl).orEmpty()
        if (bindings.size > 1 && bindings.first() !== specificDecl) {
            reporter.reportOn(
                source = specificDecl.source,
                factory = CfirErrors.MULTIPLE_COMMON_IMPLEMENTATIONS,
                a = declKindText(commonDecl),
            )
        }
'''
t = replace_once(t, old_head, new_head, 'matchesCommon head')

# ---------- 3) 宽化签名 ----------
sig_pairs = [
    ('private fun checkDeclarationKindMatch(specificDecl: CfirClass, commonDecl: CfirClass)',
     'private fun checkDeclarationKindMatch(specificDecl: CfirClassLikeDeclaration, commonDecl: CfirClassLikeDeclaration)'),
    ('private fun checkModifierMatch(specificDecl: CfirClass, commonDecl: CfirClass)',
     'private fun checkModifierMatch(specificDecl: CfirClassLikeDeclaration, commonDecl: CfirClassLikeDeclaration)'),
    ('private fun checkSuperTypeMatch(specificDecl: CfirClass, commonDecl: CfirClass)',
     'private fun checkSuperTypeMatch(specificDecl: CfirClassLikeDeclaration, commonDecl: CfirClassLikeDeclaration)'),
    ('private fun checkMemberMatch(specificDecl: CfirClass, commonDecl: CfirClass)',
     'private fun checkMemberMatch(specificDecl: CfirClassLikeDeclaration, commonDecl: CfirClassLikeDeclaration)'),
    ('private fun checkCommonDeclarationConstraints(commonDecl: CfirClass)',
     'private fun checkCommonDeclarationConstraints(commonDecl: CfirClassLikeDeclaration)'),
    ('private fun checkSpecificExtraConstraints(specificDecl: CfirClass)',
     'private fun checkSpecificExtraConstraints(specificDecl: CfirClassLikeDeclaration)'),
    ('private fun checkCommonExtraConstraints(commonDecl: CfirClass)',
     'private fun checkCommonExtraConstraints(commonDecl: CfirClassLikeDeclaration)'),
    ('private fun checkCommonSpecificAnnotations(decl: CfirClass)',
     'private fun checkCommonSpecificAnnotations(decl: CfirClassLikeDeclaration)'),
    ('private fun checkCJMPAbstractClassMembers(decl: CfirClass)',
     'private fun checkCJMPAbstractClassMembers(decl: CfirClassLikeDeclaration)'),
]
for old, new in sig_pairs:
    if old in t:
        t = t.replace(old, new, 1)
    else:
        print('WARN sig not found:', old[:60])

# ---------- 4) 种类文本统一（用宽化后的签名做锚点） ----------
old_kind = '''    private fun checkDeclarationKindMatch(specificDecl: CfirClassLikeDeclaration, commonDecl: CfirClassLikeDeclaration) {
        val specificKind = if (specificDecl.status.isAbstract) "abstract class" else "class"
        val commonKind = if (commonDecl.status.isAbstract) "abstract class" else "class"
        if (specificDecl.status.isAbstract != commonDecl.status.isAbstract) {
            reporter.reportOn(
                source = specificDecl.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_KIND,
                a = specificKind,
                b = commonKind,
            )
        }
    }'''
new_kind = '''    private fun checkDeclarationKindMatch(specificDecl: CfirClassLikeDeclaration, commonDecl: CfirClassLikeDeclaration) {
        val specificKind = declKindText(specificDecl)
        val commonKind = declKindText(commonDecl)
        if (specificKind != commonKind) {
            reporter.reportOn(
                source = specificDecl.source,
                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_KIND,
                a = specificKind,
                b = commonKind,
            )
        }
    }

    /**
     * 声明种类文本（含 class 的 abstract 区分；官方 MatchNominativeDecl 的种类语义面）。
     */
    private fun declKindText(decl: CfirClassLikeDeclaration): String = when (decl) {
        is CfirClass -> if (decl.status.isAbstract) "abstract class" else "class"
        is CfirStruct -> "struct"
        is CfirInterface -> "interface"
        is CfirEnum -> "enum"
        else -> "class"
    }'''
t = replace_once(t, old_kind, new_kind, 'kind text')

t = t.replace('''                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_MODIFIER,
                a = "class",''', '''                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_MODIFIER,
                a = declKindText(specificDecl),''', 1)
t = t.replace('''                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_SUPER_TYPE,
                a = "class",''', '''                factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_SUPER_TYPE,
                a = declKindText(specificDecl),''', 1)

# ---------- 5) checkSpecificExtraConstraints：存储消费 ----------
old_lookup = '''    private fun checkSpecificExtraConstraints(specificDecl: CfirClassLikeDeclaration) {
        val classId = specificDecl.symbol.classId
        val commonSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)
        val commonDecl = commonSymbol?.cfir as? CfirClass
'''
new_lookup = '''    private fun checkSpecificExtraConstraints(specificDecl: CfirClassLikeDeclaration) {
        // 配对结果消费存储（与 checkSpecificMatchesCommon 同源）。
        val commonDecl = context.session.cjmpMappingStorageOrNull?.commonFor(specificDecl) as? CfirClassLikeDeclaration
'''
t = replace_once(t, old_lookup, new_lookup, 'extra constraints lookup')

# ---------- 6) MULTIPLE 块的 symbolProvider 现查（搬走） ----------
old_multiple = '''        // 检查 non-exhaustive common 是否匹配 exhaustive specific
        // 这需要跨模块信息，通过 symbolProvider 查找 specific 对应声明
        val classId = commonDecl.symbol.classId
        val specificSymbols = listOfNotNull(context.session.symbolProvider.getClassLikeSymbolByClassId(classId))
        val specificDecls = specificSymbols.mapNotNull { (it.cfir as? CfirClass)?.takeIf { c -> c.status.isSpecific } }
        if (specificDecls.size > 1) {
            reporter.reportOn(
                source = commonDecl.source,
                factory = CfirErrors.MULTIPLE_COMMON_IMPLEMENTATIONS,
                a = "class",
            )
        }
    }'''
new_multiple = '''        // MULTIPLE_COMMON_IMPLEMENTATIONS 已移至 specific 侧消费（C17：matcher 第二绑定 →
        // checkSpecificMatchesCommon 消费存储上报）；此处不再 symbolProvider 现查。
        // COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH 接线属 Phase 3 item 2。
    }'''
t = replace_once(t, old_multiple, new_multiple, 'multiple block')

# ---------- 7) 删除 sema explicitly-abstract 规则 ----------
fn_start = '''    /**
     * explicitly abstract 只能用于 common/specific 抽象类。
     *
     * 对齐 C++ DiagKind::sema_explicitly_abstract_only_for_cjmp_abstract_class
     */'''
i = t.find(fn_start)
assert i != -1, 'sema rule block not found'
j = t.find('\n    }\n', i)
assert j != -1, 'sema rule end not found'
t = t[:i] + t[j + len('\n    }\n'):]
t = t.replace('\n\n\n', '\n\n')

write(p, t)
print('Phase 3 item 1 patch applied OK')
