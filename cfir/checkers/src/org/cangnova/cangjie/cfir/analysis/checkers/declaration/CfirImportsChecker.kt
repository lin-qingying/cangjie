package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.isPackageVisibleSourceImport
import org.cangnova.cangjie.cfir.resolve.providers.isUnusedImportCheckExempt
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistryOrNull
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.cfir.session.macroExpansionRegistry
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.toCjLightSourceElement
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * CFIR 文件级导入检查器。
 *
 * 该检查器负责基于导入解析阶段产出的 binding 检查 import 的可解析性、命名冲突、
 * 别名冲突以及未使用导入，并把所有诊断统一落在原始 import 指令或其具体路径片段上。
 */
object CfirImportsChecker : CfirFileChecker() {
    /**
     * 对单个 CFIR 文件执行导入相关诊断检查。
     *
     * 检查流程先从当前 session 的 import binding store 取得解析事实，再区分普通导入、
     * 别名导入、重复导入和 unresolved 导入，最后把重复导入集合传给 unused-import 检查，
     * 避免同一条 import 同时报告冲突和未使用。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        val resolvedImports = context.session.importBindingStoreOrNull?.getBindings(declaration)?.imports.orEmpty()
        val importBindingsByImport = resolvedImports.associateBy { it.importDirective }
        val conflictingNameImports = resolvedImports
            .filter { it.targets.isNotEmpty() && it.importDirective.aliasName == null }
            .filterNot { it.hasOnlyPackageTargets() }
            .groupBy { it.effectiveName }
            .collectCurrentConflictingImports()
        val conflictingAliasImports = resolvedImports
            .filter { it.targets.isNotEmpty() && it.importDirective.aliasName != null }
            .filterNot { it.hasOnlyPackageTargets() }
            .groupBy { it.importDirective.aliasName!! }
            .collectCurrentConflictingImports()

        declaration.imports.forEach { import ->
            if (import.source?.kind?.shouldSkipErrorTypeReporting == true) return@forEach
            reportImportResolutionDiagnostic(import, importBindingsByImport)

            if (import in conflictingNameImports) {
                val effectiveName = import.importedFqName?.shortName()
                if (effectiveName != null) {
                    reporter.reportOn(import.source, CfirErrors.IMPORT_CONFLICT, effectiveName)
                }
            }
            if (import in conflictingAliasImports) {
                val aliasName = import.aliasName
                if (aliasName != null) {
                    reporter.reportOn(import.source, CfirErrors.IMPORT_ALIAS_CONFLICT, aliasName)
                }
            }
        }
        val duplicateImports = declaration.imports.filterTo(linkedSetOf()) { import ->
            import in conflictingNameImports || import in conflictingAliasImports
        } + resolvedImports
            .filter { it.targets.isNotEmpty() }
            .allCurrentConflictingImports()
            .toSet()
        reportUnusedImports(declaration, duplicateImports)
    }

    /**
     * 收集当前文件内所有需要作为重复导入处理的冲突 import。
     *
     * 普通导入和别名导入分开分组，包目标导入不参与冲突判定；同名但目标签名不同的
     * import 会被视为真正冲突并进入返回集合。
     */
    private fun List<CfirResolvedImportBinding>.allCurrentConflictingImports(): Set<CfirImport> {
        val conflictingNameImports = filter { it.importDirective.aliasName == null }
            .filterNot { it.hasOnlyPackageTargets() }
            .groupBy { it.effectiveName }
            .collectAllCurrentConflictingImports()
        val conflictingAliasImports = filter { it.importDirective.aliasName != null }
            .filterNot { it.hasOnlyPackageTargets() }
            .groupBy { it.importDirective.aliasName!! }
            .collectAllCurrentConflictingImports()
        return conflictingNameImports + conflictingAliasImports
    }

    /**
     * 判断某条解析后的导入是否只解析到包目标。
     *
     * 纯包导入不会引入具体类或可调用符号，因此不参与导入冲突诊断。
     */
    private fun CfirResolvedImportBinding.hasOnlyPackageTargets(): Boolean =
        targets.isNotEmpty() && targets.all { it is CfirResolvedImportTarget.Package }

    /**
     * 从按有效名称分组的 import binding 中收集全部冲突 import。
     *
     * 该方法用于构造重复导入集合，因此同一名称组内只要存在多个稳定目标签名，
     * 组内所有 import 都会被纳入冲突集合。
     */
    private fun Map<Name, List<CfirResolvedImportBinding>>.collectAllCurrentConflictingImports(): Set<CfirImport> {
        val result = linkedSetOf<CfirImport>()
        for (bindings in values) {
            if (bindings.map { it.stableTargetSignature() }.toSet().size > 1) {
                bindings.mapTo(result) { it.importDirective }
            }
        }
        return result
    }

    /**
     * 报告单条 import 的解析失败诊断。
     *
     * 父路径片段无法解析时，诊断优先落在最早失败的路径片段；父路径可解析但终端目标
     * 不存在时，诊断落在整条 import 上并报告终端简单名。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportImportResolutionDiagnostic(
        import: CfirImport,
        importBindingsByImport: Map<CfirImport, CfirResolvedImportBinding>,
    ) {
        val importedFqName = import.importedFqName?.takeUnless { it.isRoot } ?: return
        val pathSegments = importedFqName.pathSegments()
        if (pathSegments.isEmpty()) return

        val unresolvedParentSegmentIndex = findUnresolvedParentSegmentIndex(pathSegments, import.isAllUnder)
        if (unresolvedParentSegmentIndex != null) {
            val indexFromLast = pathSegments.lastIndex - unresolvedParentSegmentIndex
            val source = import.getSourceForImportSegment(indexFromLast) ?: import.source
            reporter.reportOn(source, CfirErrors.UNRESOLVED_IMPORT, pathSegments[unresolvedParentSegmentIndex].asString())
            return
        }

        if (!import.isAllUnder && !hasResolvedTerminalImportTarget(import, importedFqName, importBindingsByImport)) {
            reporter.reportOn(import.source, CfirErrors.UNRESOLVED_IMPORT, importedFqName.shortName().asString())
            return
        }
    }

    /**
     * 未使用导入检查与 Analysis API 的 import optimization 语义保持一致：
     * - 只统计真实代码中的简单名引用，不把 import 自身计为使用；
     * - `*` 导入默认不参与 unused 判定；
     * - 已重复或已解析失败的导入不重复报 unused。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportUnusedImports(
        declaration: CfirFile,
        duplicateImports: Set<CfirImport>,
    ) {
        val localUsage = declaration.collectImportUsage(context.session)
        val packageUsage by lazy { declaration.collectPackageImportUsage(context.session) }
        val importBindingsByImport = context.session.importBindingStoreOrNull
            ?.getBindings(declaration)
            ?.imports
            .orEmpty()
            .associateBy { it.importDirective }

        for (import in declaration.imports) {
            if (import in duplicateImports) continue
            if (import.isUnusedImportCheckExempt(context.session)) continue

            val importedFqName = import.importedFqName?.takeUnless { it.isRoot } ?: continue
            if (!hasResolvedTerminalImportTarget(import, importedFqName, importBindingsByImport)) continue

            val usage = if (import.isPackageVisibleSourceImport()) packageUsage else localUsage
            if (import.isAllUnder) {
                if (import.referencesAnyStarImportedTarget(usage.targets, usage.names, context.session)) continue
                if (context.session.macroExpansionRegistry?.usedMacroNames(declaration, importedFqName).orEmpty().isNotEmpty()) continue
                reporter.reportOn(import.source, CfirErrors.UNUSED_IMPORT, importedFqName)
                continue
            }

            val importedName = import.aliasName ?: importedFqName.shortName()
            if (importedName in usage.names) continue
            if (import.aliasName == null && import.referencesAnyClassId(usage.targets.classIds, importBindingsByImport)) continue
            if (import.aliasName == null && importedFqName in usage.targets.macroPackages) continue
            if (import.referencesUsedMacroPackage(declaration, context.session, importBindingsByImport)) continue

            reporter.reportOn(import.source, CfirErrors.UNUSED_IMPORT, importedFqName)
        }
    }

    /**
     * 收集单个文件内用于 unused-import 判定的引用事实。
     */
    private fun CfirFile.collectImportUsage(session: CfirSession): ImportUsage =
        ImportUsage(
            names = collectReferencedNames() + collectMacroSurfaceReferencedNames(session),
            targets = collectReferencedImportTargets(session),
        )

    /**
     * 收集当前包全部源码文件的引用事实。
     *
     * 官方 unused-import 实现对 private import 使用文件级使用图，对非 private 的包级可见
     * import 使用整个 AST/package 使用图；这里对应 `internal import` 的同包可见语义。
     */
    private fun CfirFile.collectPackageImportUsage(session: CfirSession): ImportUsage {
        val packageFqName = packageDirective.packageFqName
        val files = session.cfirProvider.getCfirFilesByPackage(packageFqName)
        val names = linkedSetOf<Name>()
        val classIds = linkedSetOf<ClassId>()
        val callablePackages = linkedSetOf<FqName>()
        val macroPackages = linkedSetOf<FqName>()

        for (file in files) {
            val usage = file.collectImportUsage(session)
            names += usage.names
            classIds += usage.targets.classIds
            callablePackages += usage.targets.callablePackages
            macroPackages += usage.targets.macroPackages
        }

        return ImportUsage(
            names = names,
            targets = ReferencedImportTargets(
                classIds = classIds,
                callablePackages = callablePackages,
                macroPackages = macroPackages,
            ),
        )
    }

    /**
     * 收集文件正文中真实引用过的简单名。
     *
     * 收集范围覆盖命名引用和用户类型引用中的限定名片段；只有带 source 的引用会被认为
     * 来自用户源码，从而避免把合成节点或 import 指令本身误计为使用。
     */
    private fun CfirFile.collectReferencedNames(): Set<Name> {
        val result = linkedSetOf<Name>()
        accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitAnnotation(annotation: CfirAnnotation) {
                annotation.shortNameOrNull()?.let(result::add)
                annotation.typeRef.accept(this)
                annotation.arguments.forEach { it.accept(this) }
            }

            override fun visitNamedReference(namedReference: CfirNamedReference) {
                if (namedReference.source != null) {
                    result += namedReference.name
                }
                super.visitNamedReference(namedReference)
            }

            override fun visitUserTypeRef(userTypeRef: CfirUserTypeRef) {
                for (qualifierPart in userTypeRef.qualifier) {
                    if (qualifierPart.source != null) {
                        result += qualifierPart.name
                    }
                }
                super.visitUserTypeRef(userTypeRef)
            }

            override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef) {
                super.visitResolvedTypeRef(resolvedTypeRef)
                resolvedTypeRef.delegatedTypeRef?.accept(this)
            }
        })
        return result
    }

    /**
     * 收集当前源文件中 construction 阶段已经消费过的 macro surface 名称。
     *
     * 普通 checker 看到的是 macro construction 后的 final CFIR，原始 `@Macro` 节点可能已经
     * 被展开产物替换；unused-import 必须像官方 `GetUsedMacroDecls(file)` 一样，把这些
     * construction-only macro 调用也计为 import 使用。
     */
    private fun CfirFile.collectMacroSurfaceReferencedNames(session: CfirSession): Set<Name> {
        return session.macroExpansionRegistry?.usedMacroNames(this).orEmpty()
    }

    /**
     * IMPORTS 阶段已经产出的 binding 是本文件 import 可解析性的唯一事实来源。
     * checker 仅在缺失 binding 时才回退到 symbolProvider 直接查询，避免与宏导入、
     * re-export 等已解析目标再次脱节。
     */
    context(context: CheckerContext)
    private fun hasResolvedTerminalImportTarget(
        import: CfirImport,
        importedFqName: FqName,
        importBindingsByImport: Map<CfirImport, CfirResolvedImportBinding>,
    ): Boolean {
        val resolvedBinding = importBindingsByImport[import]
        if (resolvedBinding != null) {
            return resolvedBinding.targets.isNotEmpty()
        }
        return canResolveTerminalImportTarget(importedFqName)
    }

    /**
     * 收集文件正文中通过可调用符号间接引用到的所属类 ID。
     *
     * 该集合用于识别只通过成员调用触达的类导入，避免成员解析已经证明导入被使用时
     * 仍把对应类导入报告为未使用。
     */
    private fun CfirFile.collectReferencedImportTargets(session: CfirSession): ReferencedImportTargets {
        val classIds = linkedSetOf<ClassId>()
        val callablePackages = linkedSetOf<FqName>()
        val macroPackages = linkedSetOf<FqName>()
        accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitAnnotation(annotation: CfirAnnotation) {
                recordMacroAnnotationPackage(annotation, macroPackages, session)
                super.visitAnnotation(annotation)
            }

            override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef) {
                resolvedTypeRef.coneType.classId?.let(classIds::add)
                super.visitResolvedTypeRef(resolvedTypeRef)
                resolvedTypeRef.delegatedTypeRef?.accept(this)
            }

            override fun visitResolvedNamedReference(resolvedNamedReference: CfirResolvedNamedReference) {
                recordCallableImportTargets(resolvedNamedReference.resolvedSymbol, classIds, callablePackages, session)
                super.visitResolvedNamedReference(resolvedNamedReference)
            }

            override fun visitNamedReferenceWithCandidateBase(namedReferenceWithCandidateBase: CfirNamedReferenceWithCandidateBase) {
                recordCallableImportTargets(namedReferenceWithCandidateBase.candidateSymbol, classIds, callablePackages, session)
                super.visitNamedReferenceWithCandidateBase(namedReferenceWithCandidateBase)
            }
        })
        return ReferencedImportTargets(
            classIds = classIds,
            callablePackages = callablePackages,
            macroPackages = macroPackages,
        )
    }

    /**
     * 记录 annotation metadata 中的 macro surface 包使用。
     *
     * 声明宏在 construction 或 degraded 路径中可能仍以 annotation 形式保留在 final CFIR；
     * unused-import 判定必须读取 raw-builder 写入的 slot metadata，而不是把它当普通类型引用。
     */
    private fun recordMacroAnnotationPackage(
        annotation: CfirAnnotation,
        macroPackages: MutableSet<FqName>,
        session: CfirSession,
    ) {
        val annotationCall = annotation as? CfirAnnotationCall ?: return
        val qualifiedName = session.annotationMetadataRegistryOrNull
            ?.snapshot(annotationCall)
            ?.qualifiedName
            ?: return
        qualifiedName.parent().takeUnless { it.isRoot }?.let(macroPackages::add)
    }

    /**
     * 记录 resolved/candidate callable 的声明所属 class。
     *
     * 成员可能以 fake override / substitution override 形式挂在当前 receiver 类型上；
     * unused-import 判定必须同时看原始声明所属 class，才能对齐官方基于 AST target 的使用记录。
     */
    private fun recordCallableImportTargets(
        symbol: CfirBasedSymbol<*>,
        classIds: MutableSet<ClassId>,
        callablePackages: MutableSet<FqName>,
        session: CfirSession,
    ) {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return
        callablePackages += callableSymbol.callableId.packageName
        callableSymbol.callableId.classId?.let(classIds::add)
        callableSymbol.containingClassLookupTag()?.classId?.let(classIds::add)
        val originalSymbol = callableSymbol.unwrapFakeOverridesOrDelegated()
        callablePackages += originalSymbol.callableId.packageName
        originalSymbol.callableId.classId?.let(classIds::add)
        originalSymbol.containingClassLookupTag()?.classId?.let(classIds::add)

        val ownerExtend = session.extendProviderOrNull
            ?.getContainingExtend(originalSymbol)
            ?: return
        for (typeParameter in ownerExtend.typeParameters) {
            for (bound in typeParameter.bounds) {
                bound.coneTypeOrNull?.classId?.let(classIds::add)
            }
        }
        for (superTypeRef in ownerExtend.superTypeRefs) {
            superTypeRef.coneTypeOrNull?.classId?.let(classIds::add)
        }
    }

    /**
     * 判断当前 import 是否解析到了正文引用集合中的任一类目标。
     *
     * 该检查只使用 import binding store 中记录的类目标，确保 unused-import 判定与
     * 导入解析阶段看到的真实目标保持一致。
     */
    private fun CfirImport.referencesAnyClassId(
        referencedClassIds: Set<ClassId>,
        importBindingsByImport: Map<CfirImport, CfirResolvedImportBinding>,
    ): Boolean {
        val bindings = referencedClassIds.takeIf { it.isNotEmpty() } ?: return false
        return importBindingsByImport[this]
            ?.targets
            .orEmpty()
            .filterIsInstance<CfirResolvedImportTarget.ClassLike>()
            .any { it.classId in bindings }
    }

    /**
     * 判断星号导入的目标包中是否有任一 class-like 或 callable 被当前文件解析使用。
     */
    private fun CfirImport.referencesAnyStarImportedTarget(
        referencedTargets: ReferencedImportTargets,
        referencedNames: Set<Name>,
        session: CfirSession,
    ): Boolean {
        val packageFqName = importedFqName ?: return false
        return referencedTargets.classIds.any { it.packageFqName == packageFqName } ||
            packageFqName in referencedTargets.callablePackages ||
            packageFqName in referencedTargets.macroPackages ||
            referencedNames.any { name -> session.symbolProvider.hasTopLevelName(packageFqName, name) }
    }

    /**
     * 判断普通包导入是否被限定 macro 调用消费。
     *
     * macro construction 记录的是解析后的定义包使用事实；对于 `import a` 后的
     * `@a.Derive`，final CFIR 已经不一定保留原始限定调用，因此 unused-import 需要
     * 直接按导入包名查询 construction registry。
     */
    private fun CfirImport.referencesUsedMacroPackage(
        file: CfirFile,
        session: CfirSession,
        importBindingsByImport: Map<CfirImport, CfirResolvedImportBinding>,
    ): Boolean {
        val importedFqName = importedFqName ?: return false
        val registry = session.macroExpansionRegistry ?: return false
        val usedMacroNames = registry.usedMacroNames(file, importedFqName)
        if (usedMacroNames.isEmpty()) return false

        val resolvedBinding = importBindingsByImport[this]
        if (resolvedBinding != null) {
            return resolvedBinding.targets.any { it is CfirResolvedImportTarget.Package }
        }

        return session.symbolProvider.hasPackage(importedFqName)
    }

    /**
     * 判断指定包是否通过 source/delegated provider 暴露给定顶层名称。
     *
     * 这里使用 symbol provider 的 reexport 后视图，覆盖 `public import a.*` 再被
     * `import pkg.*` 消费的路径，避免星号导入只按真实声明包名判断而误报未使用。
     */
    private fun org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider.hasTopLevelName(
        packageFqName: FqName,
        name: Name,
    ): Boolean {
        if (getClassLikeSymbolByClassId(ClassId(packageFqName, name)) != null) return true
        return getTopLevelCallableSymbols(packageFqName, name).isNotEmpty()
    }

    /**
     * 当前文件中已解析引用触达的 import 目标集合。
     */
    private data class ReferencedImportTargets(
        val classIds: Set<ClassId>,
        val callablePackages: Set<FqName>,
        val macroPackages: Set<FqName>,
    )

    /**
     * unused-import 判定使用的名称引用与解析目标引用集合。
     */
    private data class ImportUsage(
        val names: Set<Name>,
        val targets: ReferencedImportTargets,
    )

    /**
     * 查找 import 父路径中第一个无法解析为包前缀的片段下标。
     *
     * 对星号导入会把完整路径都视为父路径；对普通导入则只检查终端名称之前的路径片段。
     */
    context(context: CheckerContext)
    private fun findUnresolvedParentSegmentIndex(pathSegments: List<Name>, isAllUnderImport: Boolean): Int? {
        val parentSegmentCount = if (isAllUnderImport) pathSegments.size else pathSegments.size - 1
        if (parentSegmentCount <= 0) return null

        for (index in 0 until parentSegmentCount) {
            val prefix = pathSegments.subList(0, index + 1)
            if (!canResolvePackageOrClassPrefix(prefix)) return index
        }
        return null
    }

    /**
     * 判断给定路径片段是否可以作为 import 的包前缀解析。
     *
     * 当前实现使用 session 的 symbol provider 查询包存在性，保证前缀解析与文件导入解析
     * 使用同一套符号提供入口。
     */
    context(context: CheckerContext)
    private fun canResolvePackageOrClassPrefix(prefixSegments: List<Name>): Boolean {
        val symbolProvider = context.session.symbolProvider
        if (prefixSegments.isEmpty()) return true
        val packageFqName = FqName.fromSegments(prefixSegments.map { it.asString() })
        return symbolProvider.hasPackage(packageFqName)
    }

    /**
     * 判断普通 import 的终端目标是否可以解析。
     *
     * 终端目标可以是类符号、顶层可调用符号或包；当父包本身不存在时直接返回 false，
     * 避免把不存在路径上的终端名称误判为可解析。
     */
    context(context: CheckerContext)
    private fun canResolveTerminalImportTarget(importedFqName: FqName): Boolean {
        val symbolProvider = context.session.symbolProvider
        val packageFqName = importedFqName.parent()
        val importedName = importedFqName.shortName()
        if (!packageFqName.isRoot && !symbolProvider.hasPackage(packageFqName)) return false

        val classLike = symbolProvider.getClassLikeSymbolByClassId(ClassId(packageFqName, importedName))
        val callableSymbols = symbolProvider.getTopLevelCallableSymbols(packageFqName, importedName)
        return classLike != null || callableSymbols.isNotEmpty() || symbolProvider.hasPackage(importedFqName)
    }

    /**
     * 取得 import 路径中从末尾倒数指定位置的源码元素。
     *
     * 该方法同时支持 PSI source 和 light-tree source，用于把 unresolved-import 诊断
     * 精确定位到失败的限定名片段。
     */
    private fun CfirImport.getSourceForImportSegment(indexFromLast: Int): CjSourceElement? {
        var segmentSource: CjSourceElement = source ?: return null
        repeat(indexFromLast + 1) {
            segmentSource = segmentSource.getChild(IMPORT_PARENT_TOKEN_TYPES, depth = 1) ?: return null
        }

        return if (segmentSource.elementType == CjNodeTypes.REFERENCE_EXPRESSION) {
            segmentSource
        } else {
            segmentSource.getChild(setOf(CjNodeTypes.REFERENCE_EXPRESSION), depth = 1, reverse = true)
        }
    }

    /**
     * 在 source element 的直接或限定深度子树中查找指定类型的子 source。
     *
     * PSI 与 light-tree 两种 source 表示会走不同的子节点遍历路径，但返回值统一包装为
     * CangJie source element，供诊断定位层直接使用。
     */
    private fun CjSourceElement.getChild(
        types: Set<IElementType>,
        index: Int = 0,
        depth: Int = -1,
        reverse: Boolean = false,
    ): CjSourceElement? {
        var targetIndex = index
        var result: CjSourceElement? = null

        when (this) {
            is CjLightSourceElement -> {
                forEachChildOfType(
                    root = lighterASTNode,
                    types = types,
                    depth = depth,
                    reverse = reverse,
                    getElementType = { it.tokenType },
                    getChildren = { it.childrenOf(treeStructure) },
                ) { child ->
                    if (result == null && targetIndex-- == 0) {
                        result = buildChildSourceElement(child)
                    }
                }
            }

            else -> {
                val rootPsi = psi ?: return null
                forEachChildOfType(
                    root = rootPsi,
                    types = types,
                    depth = depth,
                    reverse = reverse,
                    getElementType = { it.node?.elementType },
                    getChildren = { it.children.toList() },
                ) { child ->
                    if (result == null && targetIndex-- == 0) {
                        result = child.toCjPsiSourceElement(kind)
                    }
                }
            }
        }

        return result
    }

    /**
     * 根据 light-tree 子节点构造与当前 source element 对齐的子 source。
     *
     * light-tree 节点的偏移以原始树为基准，因此需要叠加当前 source 与根节点之间的
     * offset delta，才能得到诊断系统期望的文件内偏移。
     */
    private fun CjLightSourceElement.buildChildSourceElement(childNode: LighterASTNode): CjLightSourceElement {
        val offsetDelta = startOffset - lighterASTNode.startOffset
        return childNode.toCjLightSourceElement(
            tree = treeStructure,
            kind = kind,
            startOffset = childNode.startOffset + offsetDelta,
            endOffset = childNode.endOffset + offsetDelta,
        )
    }

    /**
     * 从 IntelliJ flyweight light-tree 结构中读取当前节点的非空子节点列表。
     */
    private fun LighterASTNode.childrenOf(tree: FlyweightCapableTreeStructure<LighterASTNode>): List<LighterASTNode> {
        val childrenRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, childrenRef)
        return childrenRef.get()?.filterNotNull().orEmpty()
    }

    /**
     * 以非递归深度优先方式遍历子树，并处理匹配指定 token 类型的节点。
     *
     * reverse 控制同一层子节点的访问方向，depth 控制最大下探深度；根节点本身不会被
     * 作为匹配结果处理，因此调用方可以直接以当前 source 或 PSI 作为遍历根。
     */
    private fun <T> forEachChildOfType(
        root: T,
        types: Set<IElementType>,
        depth: Int,
        reverse: Boolean,
        getElementType: (T) -> IElementType?,
        getChildren: (T) -> List<T>,
        processChild: (T) -> Unit,
    ) {
        val stack = mutableListOf(root to 0)

        while (stack.isNotEmpty()) {
            val (element, currentDepth) = stack.removeAt(stack.lastIndex)
            val elementType = getElementType(element)

            if (currentDepth != 0 && elementType != null && elementType in types) {
                processChild(element)
            }

            if (depth >= 0 && currentDepth == depth) continue

            val children = getChildren(element)
            val orderedChildren = if (reverse) children else children.asReversed()
            for (child in orderedChildren) {
                stack += child to (currentDepth + 1)
            }
        }
    }

    /**
     * import 路径向父级回退时允许穿过的语法节点类型集合。
     */
    private val IMPORT_PARENT_TOKEN_TYPES = setOf(
        CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
        CjNodeTypes.REFERENCE_EXPRESSION,
    )

    /**
     * 从按名称分组的 binding 中收集当前应立即报告的冲突 import。
     *
     * 与全量重复集合不同，该方法保留第一个目标签名作为基准，只把后续解析到不同目标的
     * import 加入结果，从而让冲突诊断集中落在真正引入冲突的 import 指令上。
     */
    private fun Map<Name, List<CfirResolvedImportBinding>>.collectCurrentConflictingImports(): Set<CfirImport> {
        val result = linkedSetOf<CfirImport>()
        for (bindings in values) {
            val seenTargetSignatures = linkedSetOf<String>()
            for (binding in bindings) {
                val signature = binding.stableTargetSignature()
                if (seenTargetSignatures.isNotEmpty() && signature !in seenTargetSignatures) {
                    result += binding.importDirective
                }
                seenTargetSignatures += signature
            }
        }
        return result
    }

    /**
     * 为 import binding 构造稳定的目标签名。
     *
     * 签名包含 import 文本目标、是否星号导入以及排序后的解析目标集合，用于同名 import
     * 冲突判定时消除目标顺序差异。
     */
    private fun CfirResolvedImportBinding.stableTargetSignature(): String {
        val targetSignatures = targets.map { target -> target.toString() }.sorted()
        return buildString {
            append(importDirective.importedFqName?.asString() ?: "")
            append('|')
            append(importDirective.isAllUnder)
            append('|')
            append(targetSignatures.joinToString(";"))
        }
    }
}
