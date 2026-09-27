import re
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


# ---- session factory: extend provider sees refinement deps
p = 'cfir/entrypoint/src/org/cangnova/cangjie/cfir/entrypoint/session/CfirAbstractSessionFactory.kt'
c = load(p)
rep('''                    dependencyProviders = moduleData.dependencies
                        .distinctBy { it.session }
                        .mapNotNull { it.session.extendProviderOrNull },''', '''                    // dependencies + depends-on（refinement）：CJMP specific 模块经 refinement 边看到 common extend
                    dependencyProviders = (moduleData.dependencies + moduleData.allRefinementDependencies)
                        .distinctBy { it.session }
                        .mapNotNull { it.session.extendProviderOrNull },''')
save(p, c)

# ---- resolver: extend candidates
p = 'cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpResolver.kt'
c = load(p)
rep('''            is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable -> {''', '''            is org.cangnova.cangjie.cfir.declarations.CfirExtend -> {
                // 官方 `MergeCJMPExtensions` 键：扩展类型 + `<:` 接口集；候选取 refinement 依赖中的 common extend
                val key = extendKey(specific) ?: return emptyList()
                session.extendProvider.getAllExtends().filter { candidate ->
                    candidate !== specific && extendKey(candidate) == key
                }
            }

            is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable -> {''')
rep('''object CfirCjmpResolver {''', '''object CfirCjmpResolver {
    /** extend 配对键：扩展目标类型 + 实现接口集合（渲染后的 cone 类型文本，跨 session 按结构比较）。 */
    private fun extendKey(extend: org.cangnova.cangjie.cfir.declarations.CfirExtend): Pair<String, Set<String>>? {
        val target = extend.extendedTypeRef.coneTypeOrNullSafe()?.toString() ?: return null
        val interfaces = extend.superTypeRefs.mapNotNull { it.coneTypeOrNullSafe()?.toString() }.toSet()
        return target to interfaces
    }

    private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.coneTypeOrNullSafe(): org.cangnova.cangjie.cfir.types.ConeCangJieType? =
        runCatching { (this as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef)?.coneType }.getOrNull()
''')
rep('import org.cangnova.cangjie.cfir.session.dependenciesSymbolProvider\n',
    'import org.cangnova.cangjie.cfir.session.dependenciesSymbolProvider\nimport org.cangnova.cangjie.cfir.session.extendProvider\n')
save(p, c)

# ---- runner
p = 'cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatchRunner.kt'
c = load(p)
rep('''        val matchedCommon = matchAgainstCandidates(declaration, candidates, session, storage)
        if (declaration is CfirClassLikeDeclaration) {
            if (matchedCommon is CfirClassLikeDeclaration) {
                matchMembers(declaration, matchedCommon, session, storage)
            } else {
                markMembersUnmatched(declaration, storage)
            }
        }''', '''        val matchedCommon = matchAgainstCandidates(declaration, candidates, session, storage)
        if (declaration is CfirClassLikeDeclaration) {
            if (matchedCommon is CfirClassLikeDeclaration) {
                matchMembers(declaration, matchedCommon, session, storage)
                if (declaration is CfirEnum && matchedCommon is CfirEnum) {
                    matchEnumConstructors(declaration, matchedCommon, session, storage)
                }
            } else {
                markMembersUnmatched(declaration, storage)
            }
        }
        if (declaration is CfirExtend) {
            if (matchedCommon is CfirExtend) {
                matchMemberLists(declaration.declarations, matchedCommon.declarations, session, storage)
            } else {
                markMemberListUnmatched(declaration.declarations, storage)
            }
        }''')
rep('''    fun matchMembers(
        specific: CfirClassLikeDeclaration,
        common: CfirClassLikeDeclaration,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
    ) {
        val commonMembers = common.declarations
            .filterIsInstance<CfirCallableDeclaration>()
            .filter { it !is CfirEnumConstructor && it.status.isCommon }
        for (member in specific.declarations) {''', '''    fun matchMembers(
        specific: CfirClassLikeDeclaration,
        common: CfirClassLikeDeclaration,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
    ) {
        matchMemberLists(specific.declarations, common.declarations, session, storage)
    }

    /** 成员列表配对（nominal 成员与 extend 成员共用）。 */
    private fun matchMemberLists(
        specificDeclarations: List<CfirDeclaration>,
        commonDeclarations: List<CfirDeclaration>,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
    ) {
        val commonMembers = commonDeclarations
            .filterIsInstance<CfirCallableDeclaration>()
            .filter { it !is CfirEnumConstructor && it.status.isCommon }
        for (member in specificDeclarations) {''')
rep('''    /** 未配对 nominal 的 specific 成员一律无配对（官方合并失败后成员找不到 common）。 */
    private fun markMembersUnmatched(specific: CfirClassLikeDeclaration, storage: CfirCjmpMappingStorage) {
        for (member in specific.declarations) {''', '''    /**
     * enum 构造器配对（官方 `MatchCJMPEnumConstructor`）：按名配对，带参构造器要求参数类型逐个相同；
     * common enum 非穷尽时 specific 多出的构造器合法（官方对 COMMON_NON_EXHAUSTIVE 外层静默返回）。
     */
    private fun matchEnumConstructors(
        specific: CfirEnum,
        common: CfirEnum,
        session: CfirSession,
        storage: CfirCjmpMappingStorage,
    ) {
        val commonConstructors = common.declarations.filterIsInstance<CfirEnumConstructor>()
        for (constructor in specific.declarations.filterIsInstance<CfirEnumConstructor>()) {
            val counterpart = commonConstructors.firstOrNull { candidate ->
                candidate.name == constructor.name &&
                        CfirCjmpMatcher.matchEnumConstructors(constructor, candidate)
            }
            when {
                counterpart != null -> storage.bind(constructor, counterpart)
                common.isNonExhaustive -> Unit
                else -> storage.recordUnmatched(constructor)
            }
        }
    }

    /** 未配对 nominal 的 specific 成员一律无配对（官方合并失败后成员找不到 common）。 */
    private fun markMembersUnmatched(specific: CfirClassLikeDeclaration, storage: CfirCjmpMappingStorage) {
        markMemberListUnmatched(specific.declarations, storage)
        if (specific is CfirEnum) {
            specific.declarations.filterIsInstance<CfirEnumConstructor>().forEach(storage::recordUnmatched)
        }
    }

    private fun markMemberListUnmatched(declarations: List<CfirDeclaration>, storage: CfirCjmpMappingStorage) {
        for (member in declarations) {''')
rep('import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor\n',
    'import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor\nimport org.cangnova.cangjie.cfir.declarations.CfirEnum\nimport org.cangnova.cangjie.cfir.declarations.CfirExtend\n')
save(p, c)

# ---- matcher: enum ctor comparison + extend kind
p = 'cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/cjmp/CfirCjmpMatcher.kt'
c = load(p)
rep('''    /** 官方 `MatchCJMPFunction` 对位。 */''', '''    /** 官方 `MatchEnumFuncTypes`：带参 enum 构造器参数类型逐个相同（无参构造器按名即配对）。 */
    fun matchEnumConstructors(
        specific: org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor,
        common: org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor,
    ): Boolean {
        if (specific.valueParameters.size != common.valueParameters.size) return false
        return specific.valueParameters.indices.all { i ->
            identical(
                specific.valueParameters[i].returnTypeRef.coneTypeOrNull(),
                common.valueParameters[i].returnTypeRef.coneTypeOrNull(),
                emptyMap(),
            )
        }
    }

    /** 官方 `MatchCJMPFunction` 对位。 */''')
rep('''        if (callableKind(specific) != callableKind(common)) {''', '''        if (specific is org.cangnova.cangjie.cfir.declarations.CfirExtend ||
            common is org.cangnova.cangjie.cfir.declarations.CfirExtend
        ) {
            // extend 由候选键（扩展类型 + 接口集）保证同一性
            return if (specific is org.cangnova.cangjie.cfir.declarations.CfirExtend &&
                common is org.cangnova.cangjie.cfir.declarations.CfirExtend
            ) CjmpMatchResult.Matched else CjmpMatchResult.Mismatched(CjmpMismatchKind.CLASS_KIND)
        }
        if (callableKind(specific) != callableKind(common)) {''')
save(p, c)

# ---- transformer hook for extend
p = 'cfir/resolve/src/org/cangnova/cangjie/cfir/resolve/transformers/CfirCjmpMatchingProcessor.kt'
c = load(p)
rep('''    /** 转换命名函数前执行配对。 */''', '''    /** 转换 extend 前执行配对（官方 `MergeCJMPExtensions`：扩展类型 + 接口集为键）。 */
    override fun transformExtend(
        extend: org.cangnova.cangjie.cfir.declarations.CfirExtend,
        data: Nothing?,
    ): org.cangnova.cangjie.cfir.declarations.CfirExtend {
        matchDeclaration(extend, data)
        return super.transformExtend(extend, data)
    }

    /** 转换命名函数前执行配对。 */''')
save(p, c)
print('ok')
