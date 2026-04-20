package org.cangnova.cangjie.cfir

import com.intellij.lang.LighterASTNode
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.incremental.components.EnumMatchTracker
import org.cangnova.cangjie.incremental.components.ICFileMappingTracker
import org.cangnova.cangjie.incremental.components.ImportTracker
import org.cangnova.cangjie.incremental.components.LookupTracker
import org.cangnova.cangjie.incremental.components.Position
import org.cangnova.cangjie.incremental.components.ScopeKind
import java.io.File
import java.util.concurrent.ConcurrentHashMap

abstract class CfirLookupTrackerComponent : CfirSessionComponent {
    abstract fun recordLookup(name: String, inScopes: Iterable<String>, source: CjSourceElement?, fileSource: CjSourceElement?)
    abstract fun recordLookup(name: String, inScope: String, source: CjSourceElement?, fileSource: CjSourceElement?)
    abstract fun recordDirtyDeclaration(symbol: CfirBasedSymbol<*>)
}

val CfirSession.lookupTracker: CfirLookupTrackerComponent? by CfirSession.nullableSessionComponentAccessor()

class SourcesToPathsMapper : CfirSessionComponent {
    private val sourcesToPath = mutableMapOf<LighterASTNode, String>()

    fun registerFileSource(sourceElement: CjSourceElement, path: String) {
        if (sourceElement !is CjPsiSourceElement) {
            sourcesToPath[sourceElement.treeStructure.root] = path
        }
    }

    fun getSourceFilePath(sourceElement: CjSourceElement): String? {
        return sourceElement.psiPath ?: sourcesToPath[sourceElement.treeStructure.root]
    }
}

val CfirSession.sourcesToPathsMapper: SourcesToPathsMapper by CfirSession.sessionComponentAccessor()

class IncrementalPassThroughLookupTrackerComponent(
    private val lookupTracker: LookupTracker,
    private val fileMappingTracker: ICFileMappingTracker?,
    private val sourceToFilePath: (CjSourceElement) -> String?,
) : CfirLookupTrackerComponent() {
    private val requiresPosition = lookupTracker.requiresPosition
    private val sourceToFilePathsCache = ConcurrentHashMap<CjSourceElement, String>()

    override fun recordLookup(name: String, inScopes: Iterable<String>, source: CjSourceElement?, fileSource: CjSourceElement?) {
        val definedSource = fileSource ?: source ?: return
        val path = sourceToFilePathsCache[definedSource]
            ?: sourceToFilePath(definedSource)?.also { sourceToFilePathsCache[definedSource] = it }
            ?: return

        val position = if (requiresPosition && source != null) {
            source.toPosition()
        } else {
            Position.NO_POSITION
        }

        for (scope in inScopes) {
            lookupTracker.record(path, position, scope, ScopeKind.PACKAGE, name)
        }
    }

    override fun recordLookup(name: String, inScope: String, source: CjSourceElement?, fileSource: CjSourceElement?) {
        recordLookup(name, listOf(inScope), source, fileSource)
    }

    override fun recordDirtyDeclaration(symbol: CfirBasedSymbol<*>) {
        if (fileMappingTracker == null || !symbol.isBound) return

        val declaration = symbol.cfir
        val sourcePath = when (declaration) {
            is CfirFile -> declaration.sourceFile?.path
            else -> declaration.source?.let(sourceToFilePath)
        } ?: return

        fileMappingTracker.recordSourceReferencedByCompilerPlugin(File(sourcePath))
    }
}

abstract class CfirEnumMatchTrackerComponent : CfirSessionComponent {
    abstract fun report(matchExpressionFilePath: String, enumClassFqName: String)
}

val CfirSession.enumMatchTracker: CfirEnumMatchTrackerComponent? by CfirSession.nullableSessionComponentAccessor()

fun CfirEnumMatchTrackerComponent.reportEnumUsageInMatch(path: String?, subjectType: ConeCangJieType?) {
    if (path == null || subjectType !is ConeEnumType) return
    val fqName = subjectType.classId.asString().replace(".", "$").replace("/", ".")
    report(path, fqName)
}

class IncrementalPassThroughEnumMatchTrackerComponent(
    private val enumMatchTracker: EnumMatchTracker,
) : CfirEnumMatchTrackerComponent() {
    override fun report(matchExpressionFilePath: String, enumClassFqName: String) {
        enumMatchTracker.report(matchExpressionFilePath, enumClassFqName)
    }
}

abstract class CfirImportTrackerComponent : CfirSessionComponent {
    abstract fun report(filePath: String, importedFqName: String)
}

val CfirSession.importTracker: CfirImportTrackerComponent? by CfirSession.nullableSessionComponentAccessor()

fun CfirImportTrackerComponent.reportImportDirectives(filePath: String?, importedFqName: String?) {
    if (filePath == null || importedFqName == null) return
    report(filePath, importedFqName)
}

class IncrementalPassThroughImportTrackerComponent(
    private val importTracker: ImportTracker,
) : CfirImportTrackerComponent() {
    override fun report(filePath: String, importedFqName: String) {
        importTracker.report(filePath, importedFqName)
    }
}

private val CjSourceElement.psiPath: String?
    get() = when (this) {
        is CjBinarySourceElement -> binaryFilePath
        is CjPsiSourceElement -> psi.containingFile?.virtualFile?.path
        is CjLightSourceElement -> unwrapToCjPsiSourceElement()?.psi?.containingFile?.virtualFile?.path
    }

private fun CjSourceElement.toPosition(): Position {
    val fileText = when (this) {
        is CjBinarySourceElement -> null
        is CjPsiSourceElement -> psi.containingFile?.text
        is CjLightSourceElement -> unwrapToCjPsiSourceElement()?.psi?.containingFile?.text
    } ?: return Position.NO_POSITION

    val offset = startOffset.coerceIn(0, fileText.length)
    var line = 1
    var column = 1
    for (index in 0 until offset) {
        if (fileText[index] == '\n') {
            line += 1
            column = 1
        } else {
            column += 1
        }
    }
    return Position(line, column)
}
