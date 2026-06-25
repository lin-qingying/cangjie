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

/**
 * CFIR lookup 增量追踪组件。
 */
abstract class CfirLookupTrackerComponent : CfirSessionComponent {
    /**
     * 记录名称 [name] 在多个作用域 [inScopes] 中的查找。
     */
    abstract fun recordLookup(name: String, inScopes: Iterable<String>, source: CjSourceElement?, fileSource: CjSourceElement?)

    /**
     * 记录名称 [name] 在单个作用域 [inScope] 中的查找。
     */
    abstract fun recordLookup(name: String, inScope: String, source: CjSourceElement?, fileSource: CjSourceElement?)

    /**
     * 记录被编译器插件或解析流程标记为 dirty 的声明。
     */
    abstract fun recordDirtyDeclaration(symbol: CfirBasedSymbol<*>)
}

/**
 * 从 session 中读取可为空的 lookup tracker。
 */
val CfirSession.lookupTracker: CfirLookupTrackerComponent? by CfirSession.nullableSessionComponentAccessor()

/**
 * light-tree source 到文件路径的映射表。
 */
class SourcesToPathsMapper : CfirSessionComponent {
    /**
     * light-tree 根节点到文件路径的映射。
     */
    private val sourcesToPath = mutableMapOf<LighterASTNode, String>()

    /**
     * 注册 [sourceElement] 对应的文件 [path]。
     */
    fun registerFileSource(sourceElement: CjSourceElement, path: String) {
        if (sourceElement !is CjPsiSourceElement) {
            sourcesToPath[sourceElement.treeStructure.root] = path
        }
    }

    /**
     * 读取 [sourceElement] 对应的源文件路径。
     */
    fun getSourceFilePath(sourceElement: CjSourceElement): String? {
        return sourceElement.psiPath ?: sourcesToPath[sourceElement.treeStructure.root]
    }
}

/**
 * 从 session 中读取 source-path mapper。
 */
val CfirSession.sourcesToPathsMapper: SourcesToPathsMapper by CfirSession.sessionComponentAccessor()

/**
 * 把 CFIR lookup 事件透传到增量编译 [LookupTracker]。
 */
class IncrementalPassThroughLookupTrackerComponent(
    /**
     * 增量编译使用的底层 lookup tracker。
     */
    private val lookupTracker: LookupTracker,
    /**
     * 编译器插件 dirty declaration 到源文件的映射 tracker。
     */
    private val fileMappingTracker: ICFileMappingTracker?,
    /**
     * 将 CFIR source element 解析成源文件路径的函数。
     */
    private val sourceToFilePath: (CjSourceElement) -> String?,
) : CfirLookupTrackerComponent() {
    /**
     * lookup tracker 是否要求精确位置。
     */
    private val requiresPosition = lookupTracker.requiresPosition

    /**
     * source 到文件路径的本地缓存。
     */
    private val sourceToFilePathsCache = ConcurrentHashMap<CjSourceElement, String>()

    /**
     * 记录名称在多个作用域中的查找。
     */
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

    /**
     * 记录名称在单个作用域中的查找。
     */
    override fun recordLookup(name: String, inScope: String, source: CjSourceElement?, fileSource: CjSourceElement?) {
        recordLookup(name, listOf(inScope), source, fileSource)
    }

    /**
     * 将 dirty 声明对应源码文件报告给增量文件映射 tracker。
     */
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

/**
 * enum match 增量追踪组件。
 */
abstract class CfirEnumMatchTrackerComponent : CfirSessionComponent {
    /**
     * 报告 [matchExpressionFilePath] 中匹配了 [enumClassFqName]。
     */
    abstract fun report(matchExpressionFilePath: String, enumClassFqName: String)
}

/**
 * 从 session 中读取可为空的 enum match tracker。
 */
val CfirSession.enumMatchTracker: CfirEnumMatchTrackerComponent? by CfirSession.nullableSessionComponentAccessor()

/**
 * 当 match subject 是 enum 类型时报告 enum 使用。
 */
fun CfirEnumMatchTrackerComponent.reportEnumUsageInMatch(path: String?, subjectType: ConeCangJieType?) {
    if (path == null || subjectType !is ConeEnumType) return
    val fqName = subjectType.classId.asString().replace(".", "$").replace("/", ".")
    report(path, fqName)
}

/**
 * 把 enum match 事件透传到增量编译 [EnumMatchTracker]。
 */
class IncrementalPassThroughEnumMatchTrackerComponent(
    /**
     * 增量编译使用的 enum match tracker。
     */
    private val enumMatchTracker: EnumMatchTracker,
) : CfirEnumMatchTrackerComponent() {
    /**
     * 报告 match 文件与 enum 类名。
     */
    override fun report(matchExpressionFilePath: String, enumClassFqName: String) {
        enumMatchTracker.report(matchExpressionFilePath, enumClassFqName)
    }
}

/**
 * import 指令增量追踪组件。
 */
abstract class CfirImportTrackerComponent : CfirSessionComponent {
    /**
     * 报告 [filePath] 中导入了 [importedFqName]。
     */
    abstract fun report(filePath: String, importedFqName: String)
}

/**
 * 从 session 中读取可为空的 import tracker。
 */
val CfirSession.importTracker: CfirImportTrackerComponent? by CfirSession.nullableSessionComponentAccessor()

/**
 * 安全报告 import 指令；路径或导入名缺失时跳过。
 */
fun CfirImportTrackerComponent.reportImportDirectives(filePath: String?, importedFqName: String?) {
    if (filePath == null || importedFqName == null) return
    report(filePath, importedFqName)
}

/**
 * 把 import 事件透传到增量编译 [ImportTracker]。
 */
class IncrementalPassThroughImportTrackerComponent(
    /**
     * 增量编译使用的 import tracker。
     */
    private val importTracker: ImportTracker,
) : CfirImportTrackerComponent() {
    /**
     * 报告 import 事件。
     */
    override fun report(filePath: String, importedFqName: String) {
        importTracker.report(filePath, importedFqName)
    }
}

/**
 * 从 source element 中读取可直接使用的 PSI 或二进制文件路径。
 */
private val CjSourceElement.psiPath: String?
    get() = when (this) {
        is CjBinarySourceElement -> binaryFilePath
        is CjPsiSourceElement -> psi.containingFile?.virtualFile?.path
        is CjLightSourceElement -> unwrapToCjPsiSourceElement()?.psi?.containingFile?.virtualFile?.path
    }

/**
 * 将 source element 的起始偏移转换为增量追踪位置。
 */
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
