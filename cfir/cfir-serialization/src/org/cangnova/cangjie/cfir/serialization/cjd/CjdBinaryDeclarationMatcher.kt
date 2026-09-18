package org.cangnova.cangjie.cfir.serialization.cjd

import PackageFormat.*
import PackageFormat.Package
import com.intellij.openapi.progress.ProgressManager
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext

/** 匹配失败与缺少 sidecar 条目分开报告；诊断由拥有此计划的库上下文转发。 */
enum class CjdBinaryMatchDiagnosticKind { MISSING, AMBIGUOUS, UNSUPPORTED, NON_EXPORTED, UNUSABLE_SIDECAR }

/** 一次二进制/sidecar 匹配失败的完整记录，定位信息在无索引时可为 `null`。 */
data class CjdBinaryMatchDiagnostic(
    val kind: CjdBinaryMatchDiagnosticKind,
    val declarationIndex: Int?,
    val sourceRange: CjdSourceRange?,
    val message: String,
)

/** 参数选择仍指向已唯一匹配的函数条目，不把参数位置当作独立声明匹配键。 */
data class CjdBinaryAnnotationSelection(
    val entry: CjdDeclarationEntry,
    val annotations: List<CjdAnnotation>,
    val parameterIndex: Int? = null,
)

/**
 * 某个已选中 CJO 与其精确 sibling sidecar 的不可变匹配计划。
 * 创建时只遍历原始表，不物化父/子 CFIR。每个声明在自己的 publish 前以 0-based allDecls 索引查询。
 * 生命周期跟随整个库 context/provider graph；不能跨包、跨 session 或更新 sidecar 后复用。
 */
interface CjdBinaryDeclarationMatcher {
    val diagnostics: List<CjdBinaryMatchDiagnostic>
    fun selection(declarationIndex: Int): CjdBinaryAnnotationSelection?
    /** 返回全部结构候选，即使存在歧义；只有 selection 能授权合并。 */
    fun candidates(declarationIndex: Int): List<CjdDeclarationEntry>

    companion object {
        fun create(context: CfirDeserializationContext, sidecar: CjdSidecarIndex): CjdBinaryDeclarationMatcher =
            create(context.pkg, sidecar, CjdBinaryTypeAdapter.create(context))

        /** 无 session 的原始二进制入口；适配器与 pkg 必须来自同一快照。 */
        fun create(
            pkg: Package,
            sidecar: CjdSidecarIndex,
            types: CjdBinaryTypeAdapter = CjdBinaryTypeAdapter.create(pkg),
        ): CjdBinaryDeclarationMatcher = CjdBinaryMatchPlan(pkg, sidecar, types)
    }
}

/** [CjdBinaryDeclarationMatcher] 的默认实现；构造时一次性完成整表匹配并缓存结果。 */
private class CjdBinaryMatchPlan(
    private val pkg: Package,
    private val sidecar: CjdSidecarIndex,
    private val types: CjdBinaryTypeAdapter,
) : CjdBinaryDeclarationMatcher {
    private val selected = mutableMapOf<Int, CjdBinaryAnnotationSelection>()
    private val allCandidates = mutableMapOf<Int, List<CjdDeclarationEntry>>()
    private val issues = mutableListOf<CjdBinaryMatchDiagnostic>()
    /** 先检查整张原始 owner 表，避免先发布一个 owner 的参数后才发现另一个 owner。 */
    private val conflictingOwners: Set<Int> = buildSet {
        val owners = mutableMapOf<Int, Int>()
        for (index in 0 until pkg.allDeclsLength) {
            ProgressManager.checkCanceled()
            val declaration = pkg.allDecls(index) ?: continue
            val children = memberIndices(declaration) + parameterIndices(declaration).orEmpty()
            for (child in children) {
                if (owners.putIfAbsent(child, index) != null || pkg.allDecls(child)?.isTopLevel == true) add(child)
            }
        }
    }
    override val diagnostics: List<CjdBinaryMatchDiagnostic>
    override fun selection(declarationIndex: Int): CjdBinaryAnnotationSelection? = selected[declarationIndex]
    override fun candidates(declarationIndex: Int): List<CjdDeclarationEntry> = allCandidates[declarationIndex].orEmpty()

    init {
        if (!sidecar.isUsable) {
            issue(CjdBinaryMatchDiagnosticKind.UNUSABLE_SIDECAR, null, null, "Sidecar contains blocking parser diagnostics")
        } else {
            val roots = buildList {
                for (i in 0 until pkg.allDeclsLength) {
                    // IDE 线程上遍历大包必须保留取消点。
                    ProgressManager.checkCanceled()
                    val decl = pkg.allDecls(i) ?: continue
                    if (!decl.isTopLevel || decl.kind == DeclKind.BuiltInDecl) continue
                    if (decl.kind == DeclKind.VarWithPatternDecl) {
                        // 官方把 pattern 的 binding VarDecl 作为匹配目标；绑定自身可不经过 deserializeDecl。
                        val info = if (decl.infoType == DeclInfo.VarWithPatternInfo) decl.info(VarWithPatternInfo()) as? VarWithPatternInfo else null
                        val bindings = patternBindings(info?.irrefutablePattern)
                        if (bindings.isEmpty()) issue(CjdBinaryMatchDiagnosticKind.UNSUPPORTED, i, null, "Pattern has no raw binding declaration references")
                        if (decl.cjdBaseExported()) addAll(bindings)
                    } else add(i)
                }
            }.distinct().filter { index ->
                when (isExported(pkg.allDecls(index)!!)) {
                    true -> true
                    false -> {
                        issue(CjdBinaryMatchDiagnosticKind.NON_EXPORTED, index, null, "Top-level declaration is not exported")
                        false
                    }
                    null -> {
                        issue(CjdBinaryMatchDiagnosticKind.UNSUPPORTED, index, null, "Cannot establish extend export eligibility from binary type metadata")
                        false
                    }
                }
            }
            // R13：没有顶层注解的容器不进入官方 merge。成员/参数不能绕过此门控。
            matchScope(roots, sidecar.declarations.filter { it.annotations.isNotEmpty() && it.key.kind != CjdDeclarationKind.MAIN }, emptySet())
        }
        diagnostics = issues.toList()
    }

    private fun issue(kind: CjdBinaryMatchDiagnosticKind, index: Int?, entry: CjdDeclarationEntry?, message: String) {
        issues += CjdBinaryMatchDiagnostic(kind, index, entry?.range, message)
    }

    private fun matchScope(indices: List<Int>, entries: List<CjdDeclarationEntry>, ancestors: Set<Int>) {
        val entriesByName = entries.groupBy { it.key.kind to it.key.identifier }
        val candidatesByIndex = indices.associateWith { index ->
            ProgressManager.checkCanceled()
            val decl = pkg.allDecls(index)
            if (decl == null) emptyList() else {
                val name = if (decl.kind == DeclKind.ExtendDecl) "" else decl.identifier
                entriesByName[binaryKind(decl) to name].orEmpty().filter { matches(it.key, decl) }
            }
        }
        // 有向规则可能让一个源码条目匹配多个二进制重载：同样拒绝，不允许复制注解。
        val targetCountByEntry = java.util.IdentityHashMap<CjdDeclarationEntry, Int>()
        for (candidates in candidatesByIndex.values) {
            for (entry in candidates) targetCountByEntry[entry] = (targetCountByEntry[entry] ?: 0) + 1
        }
        entries.forEach { entry ->
            if ((targetCountByEntry[entry] ?: 0) == 0) {
                val unsupported = entry.key.kind in setOf(CjdDeclarationKind.MACRO, CjdDeclarationKind.MACRO_EXPAND, CjdDeclarationKind.FINALIZER)
                issue(if (unsupported) CjdBinaryMatchDiagnosticKind.UNSUPPORTED else CjdBinaryMatchDiagnosticKind.MISSING,
                    null, entry, if (unsupported) "No faithful CJO kind mapping for ${entry.key.kind}" else "No binary declaration for ${entry.key.identifier}")
            }
        }
        for (index in indices) {
            val candidates = candidatesByIndex.getValue(index)
            allCandidates[index] = candidates
            if (index in conflictingOwners) {
                issue(CjdBinaryMatchDiagnosticKind.AMBIGUOUS, index, null, "Binary declaration has multiple ownership edges")
                continue
            }
            if (index in ancestors) {
                issue(CjdBinaryMatchDiagnosticKind.UNSUPPORTED, index, null, "Cyclic binary member ownership")
                continue
            }
            if (candidates.isEmpty()) {
                issue(CjdBinaryMatchDiagnosticKind.MISSING, index, null, "No sidecar candidate for binary declaration")
                continue
            }
            val entry = candidates.singleOrNull()
            if (entry == null || targetCountByEntry[entry] != 1) {
                issue(CjdBinaryMatchDiagnosticKind.AMBIGUOUS, index, entry, "Sidecar/binary declaration matching is not one-to-one")
                continue
            }
            if (selected.containsKey(index)) {
                selected.remove(index)
                issue(CjdBinaryMatchDiagnosticKind.AMBIGUOUS, index, entry, "Binary declaration has multiple owners")
                continue
            }
            selected[index] = CjdBinaryAnnotationSelection(entry, entry.annotations)
            val decl = pkg.allDecls(index) ?: continue
            if (entry.key is DeclarationMatchKey.Function) {
                val params = parameterIndices(decl) ?: emptyList()
                params.forEachIndexed { position, parameterIndex ->
                    allCandidates[parameterIndex] = allCandidates[parameterIndex].orEmpty() + entry
                    if (parameterIndex in conflictingOwners) {
                        issue(CjdBinaryMatchDiagnosticKind.AMBIGUOUS, parameterIndex, entry, "Parameter has multiple ownership edges")
                    } else {
                        selected[parameterIndex] = CjdBinaryAnnotationSelection(entry, entry.parameterAnnotations.getOrElse(position) { emptyList() }, position)
                    }
                }
            }
            val children = memberIndices(decl)
            if (children.isNotEmpty() || entry.members.isNotEmpty()) matchScope(children, entry.members, ancestors + index)
        }
    }

    private fun binaryKind(target: Decl): CjdDeclarationKind? = when (target.kind) {
        DeclKind.FuncDecl -> CjdDeclarationKind.FUNCTION
        DeclKind.VarDecl -> CjdDeclarationKind.VARIABLE
        DeclKind.PropDecl -> CjdDeclarationKind.PROPERTY
        DeclKind.ClassDecl -> CjdDeclarationKind.CLASS
        DeclKind.StructDecl -> CjdDeclarationKind.STRUCT
        DeclKind.InterfaceDecl -> CjdDeclarationKind.INTERFACE
        DeclKind.EnumDecl -> CjdDeclarationKind.ENUM
        DeclKind.TypeAliasDecl -> CjdDeclarationKind.TYPE_ALIAS
        DeclKind.ExtendDecl -> CjdDeclarationKind.EXTEND
        else -> null
    }

    private fun matches(source: DeclarationMatchKey, target: Decl): Boolean {
        val kind = binaryKind(target) ?: return false
        if (source.kind != kind || (kind != CjdDeclarationKind.EXTEND && source.identifier != target.identifier)) return false
        if (!matchesGeneric(source.generic, target.generic)) return false
        return when (source) {
            is DeclarationMatchKey.Function -> {
                val parameters = parameterIndices(target) ?: return false
                source.parameters.size == parameters.size && source.parameters.indices.all { i ->
                    val param = pkg.allDecls(parameters[i]) ?: return@all false
                    param.kind == DeclKind.FuncParam && source.parameters[i].name == param.identifier &&
                        source.parameters[i].type?.matchesResolved(types.typeFromField(param.type)) == true
                }
            }
            is DeclarationMatchKey.Variable -> source.type?.matchesResolved(types.typeFromField(target.type)) ?: true
            is DeclarationMatchKey.Extend -> {
                val info = if (target.infoType == DeclInfo.ExtendInfo) target.info(ExtendInfo()) as? ExtendInfo else null
                info != null && source.extendedType.matchesResolved(types.typeFromField(target.type)) &&
                    source.inheritedTypes.matchResolvedTypes(List(info.inheritedTypesLength) { types.typeFromField(info.inheritedTypes(it)) })
            }
            is DeclarationMatchKey.TypeDecl -> true
            is DeclarationMatchKey.MacroExpand -> false
        }
    }

    private fun matchesGeneric(source: CjdGenericSignature?, target: Generic?): Boolean {
        if ((source == null) != (target == null)) return false
        if (source == null || target == null) return true
        if (source.parameterNames.size != target.typeParametersLength) return false
        if (!source.parameterNames.indices.all { i ->
                val index = cjdDeclIndex(pkg, target.typeParameters(i)) ?: return@all false
                val parameter = pkg.allDecls(index)
                parameter?.kind == DeclKind.GenericParamDecl && parameter.identifier == source.parameterNames[i]
            }) return false
        if (source.constraints.isEmpty() != (target.constraintsLength == 0) || source.constraints.size > target.constraintsLength) return false
        return source.constraints.indices.all { i ->
            val constraint = target.constraints(i) ?: return@all false
            val left = source.constraints[i]
            left.type.matchesResolved(types.typeFromField(constraint.type)) &&
                left.upperBounds.matchResolvedTypes(List(constraint.uppersLength) { types.typeFromField(constraint.uppers(it)) })
        }
    }

    /** 多参数列表不能无损映射到源码单列表，拒绝而不是 flatten 后产生伪匹配。 */
    private fun parameterIndices(decl: Decl): List<Int>? {
        val info = if (decl.infoType == DeclInfo.FuncInfo) decl.info(FuncInfo()) as? FuncInfo else null
        val body = info?.funcBody ?: return null
        if (body.paramListsLength != 1) return null
        val list = body.paramLists(0) ?: return null
        val indices = List(list.paramsLength) { cjdDeclIndex(pkg, list.params(it)) }
        return if (indices.any { it == null }) null else indices.filterNotNull()
    }

    private fun memberIndices(decl: Decl): List<Int> = when (decl.infoType) {
        DeclInfo.ClassInfo -> (decl.info(ClassInfo()) as? ClassInfo)?.let { refs(it.bodyLength, it::body) }
        DeclInfo.StructInfo -> (decl.info(StructInfo()) as? StructInfo)?.let { refs(it.bodyLength, it::body) }
        DeclInfo.InterfaceInfo -> (decl.info(InterfaceInfo()) as? InterfaceInfo)?.let { refs(it.bodyLength, it::body) }
        DeclInfo.EnumInfo -> (decl.info(EnumInfo()) as? EnumInfo)?.let { refs(it.bodyLength, it::body) }
        DeclInfo.ExtendInfo -> (decl.info(ExtendInfo()) as? ExtendInfo)?.let { refs(it.bodyLength, it::body) }
        else -> null
    }.orEmpty()

    private fun refs(length: Int, get: (Int) -> UInt): List<Int> =
        (0 until length).mapNotNull { cjdDeclIndex(pkg, get(it)) }

    private fun patternBindings(pattern: Pattern?): List<Int> {
        pattern ?: return emptyList()
        if (pattern.kind == PatternKind.VarPattern) return if (pattern.exprsLength > 0) listOfNotNull(cjdDeclIndex(pkg, pattern.exprs(0))) else emptyList()
        return (0 until pattern.patternsLength).flatMap { patternBindings(pattern.patterns(it)) }
    }

    /** 官方 Node.cpp ExtendDecl::IsExportedDecl；未知目标显式拒绝，不猜测私有类型的可见性。 */
    private fun isExported(decl: Decl): Boolean? {
        if (decl.kind != DeclKind.ExtendDecl) return decl.cjdBaseExported()
        val info = if (decl.infoType == DeclInfo.ExtendInfo) decl.info(ExtendInfo()) as? ExtendInfo else null
        info ?: return null
        val extended = types.typeFromField(decl.type)
        if (extended is CjdResolvedType.Unknown) return null
        val named = extended as? CjdResolvedType.Named
        if (named?.arguments?.any { (it as? CjdResolvedType.Named)?.target?.exported == false } == true) return false
        if (named?.arguments?.any { it is CjdResolvedType.Unknown } == true) return null
        val target = named?.target
        val samePackage = target?.packageName == pkg.fullPkgName
        fun boundsExported(): Boolean? {
            val generic = decl.generic ?: return true
            for (i in 0 until generic.constraintsLength) {
                val constraint = generic.constraints(i) ?: return null
                for (j in 0 until constraint.uppersLength) {
                    val bound = types.typeFromField(constraint.uppers(j))
                    if (bound is CjdResolvedType.Unknown) return null
                    if ((bound as? CjdResolvedType.Named)?.target?.exported == false) return false
                }
            }
            return true
        }
        if (info.inheritedTypesLength == 0) {
            if (pkg.fullPkgName == "std.core") return true
            if (!samePackage) return false
            return if (target?.exported == false) false else boundsExported()
        }
        if (samePackage) return target?.exported
        var exportedInterface = false
        var unknownInterface = false
        for (i in 0 until info.inheritedTypesLength) {
            val inherited = types.typeFromField(info.inheritedTypes(i))
            if (inherited is CjdResolvedType.Unknown) unknownInterface = true
            if ((inherited as? CjdResolvedType.Named)?.target?.exported == true) exportedInterface = true
        }
        if (!exportedInterface) return if (unknownInterface) null else false
        return boundsExported()
    }
}
