package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirImport
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.toCjLightSourceElement
import org.cangnova.cangjie.source.toCjPsiSourceElement

object CfirImportsChecker : CfirFileChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFile) {
        declaration.imports.forEach { import ->
            reportImportResolutionDiagnostic(import)
        }
        reportImportConflicts(declaration.imports)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportImportResolutionDiagnostic(import: CfirImport) {
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

        if (!import.isAllUnder && !canResolveTerminalImportTarget(importedFqName)) {
            reporter.reportOn(import.source, CfirErrors.UNRESOLVED_IMPORT, importedFqName.shortName().asString())
            return
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportImportConflicts(imports: List<CfirImport>) {
        val groupedByEffectiveName = imports
            .mapNotNull { import ->
                val effectiveName = import.aliasName ?: import.importedFqName?.shortName()
                effectiveName?.let { it to import }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        for ((name, conflicts) in groupedByEffectiveName) {
            if (conflicts.size < 2) continue
            reporter.reportOn(conflicts.first().source, CfirErrors.IMPORT_CONFLICT, name)
        }

        val groupedByAlias = imports
            .mapNotNull { import ->
                val aliasName = import.aliasName ?: return@mapNotNull null
                aliasName to import
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })

        for ((alias, conflicts) in groupedByAlias) {
            if (conflicts.size < 2) continue
            reporter.reportOn(conflicts.first().source, CfirErrors.IMPORT_ALIAS_CONFLICT, alias)
        }
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

        for (packageSize in prefixSegments.size downTo 0) {
            val packageFqName = FqName.fromSegments(prefixSegments.take(packageSize).map { it.asString() })
            if (packageSize > 0 && !symbolProvider.hasPackage(packageFqName)) continue
            if (packageSize == prefixSegments.size) return true

            var classId = ClassId(packageFqName, prefixSegments[packageSize])
            if (symbolProvider.getClassLikeSymbolByClassId(classId) == null) continue

            var canResolveClassChain = true
            for (segmentIndex in (packageSize + 1) until prefixSegments.size) {
                classId = classId.createNestedClassId(prefixSegments[segmentIndex])
                if (symbolProvider.getClassLikeSymbolByClassId(classId) == null) {
                    canResolveClassChain = false
                    break
                }
            }

            if (canResolveClassChain) return true
        }

        return false
    }

    context(context: CheckerContext)
    private fun canResolveTerminalImportTarget(importedFqName: FqName): Boolean {
        val symbolProvider = context.session.symbolProvider
        val pathSegments = importedFqName.pathSegments()
        if (pathSegments.isEmpty()) return false

        val importedName = pathSegments.last()
        val parentSegments = pathSegments.dropLast(1)

        for (packageSize in parentSegments.size downTo 0) {
            val packageFqName = FqName.fromSegments(parentSegments.take(packageSize).map { it.asString() })
            if (packageSize > 0 && !symbolProvider.hasPackage(packageFqName)) continue

            if (packageSize == parentSegments.size) {
                if (symbolProvider.getClassLikeSymbolByClassId(ClassId(packageFqName, importedName)) != null) return true
                if (symbolProvider.getTopLevelCallableSymbols(packageFqName, importedName).isNotEmpty()) return true
                continue
            }

            var parentClassId = ClassId(packageFqName, parentSegments[packageSize])
            if (symbolProvider.getClassLikeSymbolByClassId(parentClassId) == null) continue

            var canResolveParentClass = true
            for (segmentIndex in (packageSize + 1) until parentSegments.size) {
                parentClassId = parentClassId.createNestedClassId(parentSegments[segmentIndex])
                if (symbolProvider.getClassLikeSymbolByClassId(parentClassId) == null) {
                    canResolveParentClass = false
                    break
                }
            }
            if (!canResolveParentClass) continue

            if (symbolProvider.getClassLikeSymbolByClassId(parentClassId.createNestedClassId(importedName)) != null) return true
        }

        return false
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
}
