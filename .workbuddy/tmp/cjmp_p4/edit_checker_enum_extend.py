root = 'D:/code/intellij/cangjie/'
s = ''


def load(p):
    global s
    s = open(root + p, encoding='utf-8', newline='').read()
    crlf = '\r\n' in s
    s = s.replace('\r\n', '\n')
    return crlf


def save(p, crlf):
    open(root + p, 'w', encoding='utf-8', newline='').write(s.replace('\n', '\r\n') if crlf else s)


def rep(old, new):
    global s
    assert s.count(old) == 1, old
    s = s.replace(old, new)


p = 'cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpCommonSideFacts.kt'
c = load(p)
rep('''    /** common 声明及其 common 成员（enum 构造器除外），附外层声明。 */
    fun commonDeclarationsWithOuter(
        topLevel: Collection<CfirDeclaration>,
    ): List<Pair<CfirDeclaration, CfirClassLikeDeclaration?>> = buildList {
        for (common in topLevel) {
            if ((common as? CfirMemberDeclaration)?.status?.isCommon != true) continue
            add(common to null)
            if (common is CfirClassLikeDeclaration) {
                for (member in common.declarations) {
                    val status = (member as? CfirMemberDeclaration)?.status ?: continue
                    if (!status.isCommon || member is CfirEnumConstructor) continue
                    add(member to common)
                }
            }
        }
    }''', '''    /**
     * common 声明及其 common 成员（nominal 与 extend 成员；common enum 的全部构造器——官方构造器随外层携带
     * COMMON），附外层声明。
     */
    fun commonDeclarationsWithOuter(
        topLevel: Collection<CfirDeclaration>,
    ): List<Pair<CfirDeclaration, CfirDeclaration?>> = buildList {
        for (common in topLevel) {
            if ((common as? CfirMemberDeclaration)?.status?.isCommon != true) continue
            add(common to null)
            val members = when (common) {
                is CfirClassLikeDeclaration -> common.declarations
                is CfirExtend -> common.declarations
                else -> emptyList()
            }
            for (member in members) {
                if (member is CfirEnumConstructor) {
                    add(member to common)
                    continue
                }
                val status = (member as? CfirMemberDeclaration)?.status ?: continue
                if (!status.isCommon) continue
                add(member to common)
            }
        }
    }''')
rep('''    fun mustReportNotMatched(
        common: CfirDeclaration,
        outer: CfirClassLikeDeclaration?,
        storage: CfirCjmpMappingStorage,
    ): Boolean {
        if (storage.specificBindingsFor(common).isNotEmpty()) return false''', '''    fun mustReportNotMatched(
        common: CfirDeclaration,
        outer: CfirDeclaration?,
        storage: CfirCjmpMappingStorage,
    ): Boolean {
        if (storage.specificBindingsFor(common).isNotEmpty()) return false
        if (common is CfirEnumConstructor) {
            // 官方：外层 enum 带默认实现且无 specific 实现时构造器无需配对
            return !(outer != null && outer.cjmpHasCommonDefault() && storage.specificBindingsFor(outer).isEmpty())
        }''')
rep('''    fun declName(declaration: CfirDeclaration): Name = when (declaration) {
        is CfirClassLikeDeclaration -> declaration.name''', '''    fun declName(declaration: CfirDeclaration): Name = when (declaration) {
        is CfirClassLikeDeclaration -> declaration.name
        is CfirEnumConstructor -> declaration.name
        is CfirExtend -> (declaration.extendedTypeRef as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef)
            ?.coneType?.let { type ->
                (type as? org.cangnova.cangjie.cfir.types.ConeClassLikeType)?.classId?.shortClassName
                    ?: Name.identifier(type.toString())
            } ?: Name.special("<extend>")''')
rep('''    /** 官方 `DiagNotMatchedDecl` 的第二参数："constructor" 或 "Kind 'name'"。 */
    fun declInfo(declaration: CfirDeclaration): String {
        if (declaration is CfirConstructor) return "constructor"
        return "${declKind(declaration)} '${declName(declaration).asString()}'"
    }''', '''    /**
     * 官方 `DiagNotMatchedDecl` 的第二参数："constructor"、"enum 'E' constructor 'C'" 或 "Kind 'name'"。
     *
     * @param outer enum 构造器所在 enum（其余声明不需要）。
     */
    fun declInfo(declaration: CfirDeclaration, outer: CfirDeclaration? = null): String {
        if (declaration is CfirConstructor) return "constructor"
        if (declaration is CfirEnumConstructor) {
            val enumName = outer?.let(::declName)?.asString()
                ?: declaration.symbol.callableId.classId?.shortClassName?.asString().orEmpty()
            return "enum '$enumName' constructor '${declaration.name.asString()}'"
        }
        return "${declKind(declaration)} '${declName(declaration).asString()}'"
    }''')
save(p, c)

p = 'cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt'
c = load(p)
rep('''                    CfirCjmpCommonSideFacts.mustReportNotMatched(common, outer, storage) -> reporter.reportOn(
                        source = source,
                        factory = CfirErrors.NOT_MATCHED,
                        a = "common",
                        b = CfirCjmpCommonSideFacts.declInfo(common),''', '''                    CfirCjmpCommonSideFacts.mustReportNotMatched(common, outer, storage) -> reporter.reportOn(
                        source = source,
                        factory = CfirErrors.NOT_MATCHED,
                        a = "common",
                        b = CfirCjmpCommonSideFacts.declInfo(common, outer),''')
rep('''    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val member = declaration as? CfirMemberDeclaration ?: return
        if (!member.status.isSpecific) return
        if (declaration is CfirEnumConstructor || declaration is CfirTypeAlias) return''', '''    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        if (declaration is CfirEnumConstructor) {
            checkEnumConstructor(declaration)
            return
        }
        val member = declaration as? CfirMemberDeclaration ?: return
        if (!member.status.isSpecific) return
        if (declaration is CfirTypeAlias) return''')
rep('''    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportUnmatched(declaration: CfirDeclaration, storage: CfirCjmpMappingStorage) {''', '''    /**
     * specific enum 的构造器（官方构造器随外层携带 SPECIFIC）：无 common 对应构造器即 NOT_MATCHED
     *（common enum 非穷尽时的多出构造器由配对阶段静默）。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkEnumConstructor(declaration: CfirEnumConstructor) {
        val enum = context.findClosestDeclaration<CfirEnum>() ?: return
        if (!enum.status.isSpecific) return
        if (!CjmpGate.isEnabled(context)) return
        if (context.session.cjmpSettings.mode != CfirCjmpMode.SPECIFIC) return
        val storage = context.session.cjmpMappingStorageOrNull ?: return
        if (!storage.isUnmatched(declaration)) return
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.NOT_MATCHED,
            a = "specific",
            b = CfirCjmpCommonSideFacts.declInfo(declaration, enum),
            c = "common",
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportUnmatched(declaration: CfirDeclaration, storage: CfirCjmpMappingStorage) {''')
rep('''        if (!checkModifiers(specific, specificStatus, common, isNominal = false)) return

        val specificAnnotations''', '''        if (!checkModifiers(specific, specificStatus, common, isNominal = false)) return

        // 官方 `CheckCommonSpecificGenericMatch`：specific 泛型约束须不严于 common（按位置映射，D6）
        if (specific is CfirCallableDeclaration && common is CfirCallableDeclaration) {
            CfirOverrideChecker.checkGenericConstraintCompatibility(specific, listOf(common.symbol))
        }

        val specificAnnotations''')
rep('import org.cangnova.cangjie.cfir.session.CfirCjmpMode\n',
    'import org.cangnova.cangjie.cfir.session.CfirCjmpMode\nimport org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration\n')
save(p, c)

p = 'compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/CjmpDeserializedCommonSideReporter.kt'
c = load(p)
rep('''                        "'common' ${CfirCjmpCommonSideFacts.declInfo(common)} can not find 'specific' match"''',
    '''                        "'common' ${CfirCjmpCommonSideFacts.declInfo(common, outer)} can not find 'specific' match"''')
rep('''                names.getTopLevelCallableNamesInPackage(packageFqName).orEmpty().forEach { name ->
                    provider.getTopLevelCallableSymbols(packageFqName, name).forEach { add(it.cfir) }
                }''', '''                names.getTopLevelCallableNamesInPackage(packageFqName).orEmpty().forEach { name ->
                    provider.getTopLevelCallableSymbols(packageFqName, name).forEach { add(it.cfir) }
                }
                addAll(session.extendProvider.getExtendsInPackage(packageFqName))''')
rep('import org.cangnova.cangjie.cfir.session.dependenciesSymbolProvider\n',
    'import org.cangnova.cangjie.cfir.session.dependenciesSymbolProvider\nimport org.cangnova.cangjie.cfir.session.extendProvider\n')
save(p, c)
print('ok')
