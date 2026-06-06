package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.isReexportingSourceImport
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.toCjLightSourceElement
import org.cangnova.cangjie.source.toCjPsiSourceElement

object CfirImportsChecker : CfirFileChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        val resolvedImports = context.session.importBindingStoreOrNull?.getBindings(declaration)?.imports.orEmpty()
        val importBindingsByImport = resolvedImports.associateBy { it.importDirective }
        val conflictingNameImports = resolvedImports
            .filter { it.targets.isNotEmpty() && it.importDirective.aliasName == null }
            .groupBy { it.effectiveName }
            .collectCurrentConflictingImports()
        val conflictingAliasImports = resolvedImports
            .filter { it.targets.isNotEmpty() && it.importDirective.aliasName != null }
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

    private fun List<CfirResolvedImportBinding>.allCurrentConflictingImports(): Set<CfirImport> {
        val conflictingNameImports = filter { it.importDirective.aliasName == null }
            .groupBy { it.effectiveName }
            .collectAllCurrentConflictingImports()
        val conflictingAliasImports = filter { it.importDirective.aliasName != null }
            .groupBy { it.importDirective.aliasName!! }
            .collectAllCurrentConflictingImports()
        return conflictingNameImports + conflictingAliasImports
    }

    private fun Map<Name, List<CfirResolvedImportBinding>>.collectAllCurrentConflictingImports(): Set<CfirImport> {
        val result = linkedSetOf<CfirImport>()
        for (bindings in values) {
            if (bindings.map { it.stableTargetSignature() }.toSet().size > 1) {
                bindings.mapTo(result) { it.importDirective }
            }
        }
        return result
    }

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
        val psiFile = declaration.source?.psi as? CjFile ?: return
        val referencedNames = PsiTreeUtil.collectElementsOfType(psiFile, CjSimpleNameExpression::class.java)
            .asSequence()
            .filter { reference: CjSimpleNameExpression -> reference.getStrictParentOfType<CjImportDirective>() == null }
            .map(CjSimpleNameExpression::referencedNameAsName)
            .toSet()
        val referencedClassIds = declaration.collectReferencedClassIds()
        val importBindingsByImport = context.session.importBindingStoreOrNull
            ?.getBindings(declaration)
            ?.imports
            .orEmpty()
            .associateBy { it.importDirective }

        for (import in declaration.imports) {
            if (import in duplicateImports) continue
            if (import.isAllUnder) continue
            if (import.isReexportingSourceImport()) continue

            val importedFqName = import.importedFqName?.takeUnless { it.isRoot } ?: continue
            if (!hasResolvedTerminalImportTarget(import, importedFqName, importBindingsByImport)) continue

            val importedName = import.aliasName ?: importedFqName.shortName()
            if (importedName in referencedNames) continue
            if (import.referencesAnyClassId(referencedClassIds, importBindingsByImport)) continue

            reporter.reportOn(import.source, CfirErrors.UNUSED_IMPORT, importedFqName)
        }
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
        if (importedFqName.asString() == "untitled89.b.a11") {
            System.err.println(
                "DEBUG_A11 checker bindingTargets=" +
                    resolvedBinding?.targets.orEmpty().joinToString { it::class.simpleName ?: it::class.java.name } +
                    " bindingPresent=${resolvedBinding != null}"
            )
        }
        if (resolvedBinding != null) {
            return resolvedBinding.targets.any { target -> target !is CfirResolvedImportTarget.Package }
        }
        return canResolveTerminalImportTarget(importedFqName)
    }

    private fun CfirFile.collectReferencedClassIds(): Set<ClassId> {
        val result = linkedSetOf<ClassId>()
        accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitResolvedNamedReference(resolvedNamedReference: CfirResolvedNamedReference) {
                val callableSymbol = resolvedNamedReference.resolvedSymbol as? CfirCallableSymbol<*>
                callableSymbol?.containingClassLookupTag()?.classId?.let(result::add)
                super.visitResolvedNamedReference(resolvedNamedReference)
            }
        })
        return result
    }

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

    context(context: CheckerContext)
    private fun canResolvePackageOrClassPrefix(prefixSegments: List<Name>): Boolean {
        val symbolProvider = context.session.symbolProvider
        if (prefixSegments.isEmpty()) return true
        val packageFqName = FqName.fromSegments(prefixSegments.map { it.asString() })
        return symbolProvider.hasPackage(packageFqName)
    }

    context(context: CheckerContext)
    private fun canResolveTerminalImportTarget(importedFqName: FqName): Boolean {
        val symbolProvider = context.session.symbolProvider
        val packageFqName = importedFqName.parent()
        val importedName = importedFqName.shortName()
        if (!packageFqName.isRoot && !symbolProvider.hasPackage(packageFqName)) return false

        val classLike = symbolProvider.getClassLikeSymbolByClassId(ClassId(packageFqName, importedName))
        val callableSymbols = symbolProvider.getTopLevelCallableSymbols(packageFqName, importedName)
        if (importedFqName.asString() == "untitled89.b.a11") {
            System.err.println(
                "DEBUG_A11 checkerFallback classLike=${classLike != null} callables=${callableSymbols.size}"
            )
        }
        return classLike != null || callableSymbols.isNotEmpty()
    }

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

    private fun CjLightSourceElement.buildChildSourceElement(childNode: LighterASTNode): CjLightSourceElement {
        val offsetDelta = startOffset - lighterASTNode.startOffset
        return childNode.toCjLightSourceElement(
            tree = treeStructure,
            kind = kind,
            startOffset = childNode.startOffset + offsetDelta,
            endOffset = childNode.endOffset + offsetDelta,
        )
    }

    private fun LighterASTNode.childrenOf(tree: FlyweightCapableTreeStructure<LighterASTNode>): List<LighterASTNode> {
        val childrenRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, childrenRef)
        return childrenRef.get()?.filterNotNull().orEmpty()
    }

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

    private val IMPORT_PARENT_TOKEN_TYPES = setOf(
        CjNodeTypes.DOT_QUALIFIED_EXPRESSION,
        CjNodeTypes.REFERENCE_EXPRESSION,
    )

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
