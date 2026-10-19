package org.cangnova.cangjie.analysis.low.level.api.cfir.macro

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ModificationTracker
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroAppliedDeclarationReplacement
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroAppliedExpressionReplacement
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicReference

/** 宏快照构造所需的 CFIR 结构与短读访问中捕获的 PSI 身份信息。 */
internal data class LLCfirMacroSemanticSnapshotConstructionInputs(
    val declarationsByFile: IdentityHashMap<CfirFile, List<CfirDeclaration>>,
    val fileByElement: IdentityHashMap<CfirElement, CfirFile>,
    val declarationPsiByCfir: IdentityHashMap<CfirDeclaration, CjDeclaration?>,
    val macroGeneratedDeclarations: Set<CfirDeclaration>,
    val declarationReplacementSources: List<LLCfirMacroDeclarationReplacementSource>,
    val expressionReplacementSources: List<LLCfirMacroExpressionReplacementSource>,
)

/** 物理声明 carrier 在 PSI 读访问中捕获的源 PSI 与所属文件。 */
internal data class LLCfirMacroDeclarationReplacementSource(
    val replacement: MacroAppliedDeclarationReplacement,
    val sourcePsi: CjDeclaration,
    val physicalSourceFile: CjFile?,
)

/** 表达式 carrier 在 PSI 读访问中捕获的源调用 PSI 与所属文件。 */
internal data class LLCfirMacroExpressionReplacementSource(
    val replacement: MacroAppliedExpressionReplacement,
    val sourcePsi: CjMacroExpression,
    val sourceFile: CjFile,
)

private data class PhysicalDeclarationSource(
    val sourcePsi: CjDeclaration,
    val sourceFile: CjFile,
)

/** 在 PSI 读访问中捕获的来源区间；发布到索引后不再访问 PSI。 */
private data class MacroGeneratedSourceRange(
    val sourceTreeIdentity: Any,
    val startOffset: Int,
    val endOffset: Int,
    val origins: Set<Long>,
)

/** 在 PSI 读访问中捕获的顶层声明来源信息。 */
private data class MacroGeneratedDeclarationSource(
    val declaration: CfirDeclaration,
    val sourceTreeIdentity: Any?,
    val startOffset: Int,
    val endOffset: Int,
    val exactOrigins: Set<Long>?,
)

private data class MacroSourceIntervalNode(
    val range: MacroGeneratedSourceRange,
    val left: MacroSourceIntervalNode?,
    val right: MacroSourceIntervalNode?,
    val maxEndOffset: Int,
    val minWidth: Int,
)

/**
 * 由 PSI 读访问捕获数据构建的宏来源区间索引。
 *
 * 以文件/LightTree 身份分桶，并用带子树最大结束偏移与最小区间宽度的平衡区间树查询
 * 包含关系。精确 source 键在读访问中按 registry 原有相等语义预先解析，优先级高于范围投影。
 */
private class MacroGeneratedSourceIndex(
    ranges: List<MacroGeneratedSourceRange>,
    private val resolvedOriginIds: Set<Long>,
) {
    private val intervalsBySourceTree = IdentityHashMap<Any, MacroSourceIntervalNode>()

    init {
        val rangesBySourceTree = IdentityHashMap<Any, MutableList<MacroGeneratedSourceRange>>()
        ranges.forEach { range ->
            ProgressManager.checkCanceled()
            rangesBySourceTree.getOrPut(range.sourceTreeIdentity, ::mutableListOf).add(range)
        }
        rangesBySourceTree.forEach { (treeIdentity, treeRanges) ->
            val sortedRanges = treeRanges.sortedWith(
                compareBy<MacroGeneratedSourceRange> { it.startOffset }.thenBy { it.endOffset },
            )
            intervalsBySourceTree[treeIdentity] = buildIntervalTree(sortedRanges, 0, sortedRanges.size)
        }
    }

    fun hasUniqueOrigin(source: MacroGeneratedDeclarationSource): Boolean {
        source.exactOrigins?.let { origins ->
            val uniqueOrigin = origins.singleOrNull() ?: return false
            return uniqueOrigin in resolvedOriginIds
        }
        val sourceTreeIdentity = source.sourceTreeIdentity ?: return false
        val root = intervalsBySourceTree[sourceTreeIdentity] ?: return false
        var narrowestWidth = Int.MAX_VALUE
        val matchingOrigins = linkedSetOf<Long>()

        fun collectMatchingRanges(node: MacroSourceIntervalNode?) {
            if (node == null || node.maxEndOffset < source.endOffset || node.minWidth > narrowestWidth) return
            ProgressManager.checkCanceled()
            if (node.range.startOffset <= source.startOffset && node.range.endOffset >= source.endOffset) {
                val width = node.range.endOffset - node.range.startOffset
                when {
                    width < narrowestWidth -> {
                        narrowestWidth = width
                        matchingOrigins.clear()
                        matchingOrigins += node.range.origins
                    }
                    width == narrowestWidth -> matchingOrigins += node.range.origins
                }
            }
            if (node.range.startOffset > source.startOffset) {
                collectMatchingRanges(node.left)
            } else {
                collectMatchingRanges(node.left)
                collectMatchingRanges(node.right)
            }
        }

        collectMatchingRanges(root)
        val uniqueOrigin = matchingOrigins.singleOrNull() ?: return false
        return uniqueOrigin in resolvedOriginIds
    }

    private fun buildIntervalTree(
        ranges: List<MacroGeneratedSourceRange>,
        fromIndex: Int,
        untilIndex: Int,
    ): MacroSourceIntervalNode {
        ProgressManager.checkCanceled()
        val middleIndex = (fromIndex + untilIndex) ushr 1
        val left = if (fromIndex < middleIndex) buildIntervalTree(ranges, fromIndex, middleIndex) else null
        val right = if (middleIndex + 1 < untilIndex) buildIntervalTree(ranges, middleIndex + 1, untilIndex) else null
        val range = ranges[middleIndex]
        val width = range.endOffset - range.startOffset
        return MacroSourceIntervalNode(
            range = range,
            left = left,
            right = right,
            maxEndOffset = maxOf(range.endOffset, left?.maxEndOffset ?: Int.MIN_VALUE, right?.maxEndOffset ?: Int.MIN_VALUE),
            minWidth = minOf(width, left?.minWidth ?: Int.MAX_VALUE, right?.minWidth ?: Int.MAX_VALUE),
        )
    }
}

/** 返回 PSI 文件身份或纯 LightTree 结构身份；调用者必须处于 PSI 读访问中。 */
private fun sourceTreeIdentity(source: CjSourceElement): Any? = when (source) {
    is CjPsiSourceElement -> source.psi.containingFile
    is CjLightSourceElement -> source.unwrapToCjPsiSourceElement()?.psi?.containingFile ?: source.treeStructure
    is CjBinarySourceElement -> null
}

/**
 * 已完成宏构造的源码模块 CFIR 视图。
 *
 * 所有索引都从同一批最终文件构建。PSI 文件身份、CFIR 声明容器、名称过滤与符号查询因此共享一个原子发布边界。
 */
class LLCfirMacroSemanticSnapshot internal constructor(
    sourceFiles: List<CjFile>,
    /** 本次发布的完整 expanded CFIR 文件集合。 */
    val files: List<CfirFile>,
    /** 当前快照的宏来源和生成源码位置映射。 */
    val registry: MacroExpansionRegistry,
    constructionInputs: LLCfirMacroSemanticSnapshotConstructionInputs,
) {
    /** 原始物理 PSI 文件到最终 CFIR 文件的身份映射。 */
    private val filesBySourceIdentity: Map<CjFile, CfirFile>

    /** 发布时冻结的 surface 索引，避免后续 construction 写入改变已发布导航目标。 */
    private val originSurfacesById: Map<Long, MacroSurface> = Collections.unmodifiableMap(registry.originSurfaceById)

    /** 发布时冻结的生成 source 身份集合。 */
    private val generatedSourceIdentities: Set<AbstractCjSourceElement> = registry.generatedSources

    /** 每个最终文件所属的包。 */
    val filesByPackage: Map<FqName, List<CfirFile>>

    /** 最终文件中的 class-like 声明，按稳定 ClassId 聚合。 */
    private val classifiersByClassId: Map<ClassId, List<CfirClassLikeDeclaration>>

    /** 最终文件中的声明，按 PSI 对象身份索引；值保留宏声明的一对多语义。 */
    private val declarationsBySourceIdentity: Map<CjDeclaration, List<CfirDeclaration>>

    /** 最终文件中的 class-like 声明，按 PSI 对象身份索引。 */
    private val classifiersBySourceIdentity: Map<CjClassLikeDeclaration, List<CfirClassLikeDeclaration>>

    /** 经 stable splice 身份关系确认的物理声明 carrier PSI。 */
    private val physicalSourceByDeclarationIdentity: Map<CfirDeclaration, List<PhysicalDeclarationSource>>

    /** 即使替换结果为空也保留其 carrier 身份，区分零输出 carrier 与普通未索引声明。 */
    private val declarationReplacementCarriers: Set<CjDeclaration>

    /** 经 stable splice 身份关系确认的物理表达式宏调用 PSI 到最终表达式映射。 */
    private val expressionsByMacroSourceIdentity: Map<CjMacroExpression, CfirExpression>

    /** 按物理源码文件身份索引的表达式宏替换映射，不在查询时回读 PSI 树。 */
    private val expressionsBySourceFileIdentity: Map<CjFile, Map<CjMacroExpression, CfirExpression>>

    /** 最终文件中的顶层 callable 声明，按包名与名称聚合。 */
    private val callablesById: Map<CallableId, List<CfirCallableDeclaration>>

    /** 最终源码模块中的顶层 classifier 与 callable 名称。 */
    private val classifierNamesByPackage: Map<FqName, Set<Name>>
    private val callableNamesByPackage: Map<FqName, Set<Name>>
    /** 最终快照中由宏来源映射确认的顶层 class-like 名称。 */
    private val macroGeneratedClassifierNamesByPackage: Map<FqName, Set<Name>>
    /** 最终快照中由宏来源映射确认的顶层 callable 名称。 */
    private val macroGeneratedCallableNamesByPackage: Map<FqName, Set<Name>>

    /** 展开后每个 CFIR 声明对应的最终容器文件，使用对象身份避免结构相等误匹配。 */
    private val containerByDeclaration: Map<CfirDeclaration, CfirFile>

    /** 最终 CFIR 声明到其最近宏替换来源的身份索引；值可多来源以显式保留歧义。 */
    private val macroSurfaceIdsByDeclarationIdentity: Map<CfirDeclaration, Set<Long>>

    init {
        require(sourceFiles.size == files.size) {
            "sourceFiles size (${sourceFiles.size}) must match expanded files size (${files.size})"
        }

        val sourceIdentityMap = IdentityHashMap<CjFile, CfirFile>(sourceFiles.size)
        sourceFiles.forEachIndexed { index, sourceFile ->
            check(sourceIdentityMap.put(sourceFile, files[index]) == null) {
                "A source PSI file occurs more than once in a macro semantic snapshot."
            }
        }
        filesBySourceIdentity = Collections.unmodifiableMap(sourceIdentityMap)

        val packageFiles = linkedMapOf<FqName, MutableList<CfirFile>>()
        val classifiers = linkedMapOf<ClassId, MutableList<CfirClassLikeDeclaration>>()
        val declarationsBySource = IdentityHashMap<CjDeclaration, MutableList<CfirDeclaration>>()
        val classifiersBySource = IdentityHashMap<CjClassLikeDeclaration, MutableList<CfirClassLikeDeclaration>>()
        val physicalSourceByDeclaration = IdentityHashMap<CfirDeclaration, MutableList<PhysicalDeclarationSource>>()
        val replacementCarriers = Collections.newSetFromMap(IdentityHashMap<CjDeclaration, Boolean>())
        val expressionsByMacroSource = IdentityHashMap<CjMacroExpression, CfirExpression>()
        val expressionsBySourceFile = IdentityHashMap<CjFile, IdentityHashMap<CjMacroExpression, CfirExpression>>()
        val callables = linkedMapOf<CallableId, MutableList<CfirCallableDeclaration>>()
        val classifierNames = linkedMapOf<FqName, MutableSet<Name>>()
        val callableNames = linkedMapOf<FqName, MutableSet<Name>>()
        val macroGeneratedClassifierNames = linkedMapOf<FqName, MutableSet<Name>>()
        val macroGeneratedCallableNames = linkedMapOf<FqName, MutableSet<Name>>()
        val declarationContainers = IdentityHashMap<CfirDeclaration, CfirFile>()

        constructionInputs.fileByElement.forEach { (element, file) ->
            if (element is CfirDeclaration) declarationContainers[element] = file
        }

        for (file in files) {
            val packageFqName = file.packageDirective.packageFqName
            packageFiles.getOrPut(packageFqName, ::mutableListOf).add(file)

            for (declaration in file.declarations) {
                val macroGenerated = declaration in constructionInputs.macroGeneratedDeclarations
                if (declaration is CfirClassLikeDeclaration) {
                    classifiers.getOrPut(declaration.symbol.classId, ::mutableListOf).add(declaration)
                    classifierNames.getOrPut(packageFqName, ::linkedSetOf).add(declaration.symbol.name)
                    if (macroGenerated) {
                        macroGeneratedClassifierNames.getOrPut(packageFqName, ::linkedSetOf).add(declaration.symbol.name)
                    }
                }
                if (declaration is CfirCallableDeclaration) {
                    val callableName = declaration.symbol.name
                    callables.getOrPut(CallableId(packageFqName, callableName), ::mutableListOf).add(declaration)
                    callableNames.getOrPut(packageFqName, ::linkedSetOf).add(callableName)
                    if (macroGenerated) {
                        macroGeneratedCallableNames.getOrPut(packageFqName, ::linkedSetOf).add(callableName)
                    }
                }
            }

            for (declaration in constructionInputs.declarationsByFile[file].orEmpty()) {
                // PSI 身份索引表达真实源码声明的 owner 关系；synthetic/fake source（例如 implicit constructor）
                // 可以共享 class PSI，但不能把 class 的唯一 source owner 基数扩成二。
                if (declaration.source?.kind != CjRealSourceElementKind) continue
                val sourcePsi = constructionInputs.declarationPsiByCfir[declaration] ?: continue
                declarationsBySource.getOrPut(sourcePsi, ::mutableListOf).addIdentity(declaration)
                if (declaration is CfirClassLikeDeclaration && sourcePsi is CjClassLikeDeclaration) {
                    classifiersBySource.getOrPut(sourcePsi, ::mutableListOf).addIdentity(declaration)
                }
            }
        }

        for (capturedSource in constructionInputs.declarationReplacementSources) {
            val replacement = capturedSource.replacement
            val sourcePsi = capturedSource.sourcePsi
            replacementCarriers += sourcePsi
            for (semanticDeclaration in replacement.replacements) {
                check(declarationContainers.containsKey(semanticDeclaration)) {
                    "A recorded macro replacement must belong to the published semantic snapshot."
                }
                declarationsBySource.getOrPut(sourcePsi, ::mutableListOf).addIdentity(semanticDeclaration)
                if (semanticDeclaration is CfirClassLikeDeclaration && sourcePsi is CjClassLikeDeclaration) {
                    classifiersBySource.getOrPut(sourcePsi, ::mutableListOf).addIdentity(semanticDeclaration)
                }
                capturedSource.physicalSourceFile?.let { sourceFile ->
                    val sourceSemanticFile = filesBySourceIdentity[sourceFile]
                        ?: error("A declaration macro carrier must belong to a source file in this snapshot.")
                    check(declarationContainers[semanticDeclaration] === sourceSemanticFile) {
                        "A declaration macro replacement must remain in the expanded file for its physical source file."
                    }
                    physicalSourceByDeclaration.getOrPut(semanticDeclaration, ::mutableListOf)
                        .addPhysicalSource(sourcePsi, sourceFile)
                }
            }
        }

        for (capturedSource in constructionInputs.expressionReplacementSources) {
            val replacement = capturedSource.replacement
            val sourcePsi = capturedSource.sourcePsi
            val sourceFile = capturedSource.sourceFile
            val sourceSemanticFile = filesBySourceIdentity[sourceFile]
                ?: error("A physical expression macro call must belong to a source file in this snapshot.")
            check(constructionInputs.fileByElement[replacement.replacement] === sourceSemanticFile) {
                "A macro expression replacement must belong to the expanded file for its physical source file."
            }
            check(expressionsByMacroSource.put(sourcePsi, replacement.replacement) == null) {
                "A physical macro expression PSI cannot map to multiple final CFIR expressions."
            }
            val sourceFileMappings = expressionsBySourceFile.getOrPut(sourceFile) { IdentityHashMap() }
            check(sourceFileMappings.put(sourcePsi, replacement.replacement) == null) {
                "A physical macro expression PSI cannot map to multiple final CFIR expressions in one source file."
            }
        }

        val directSurfaceIdsByDeclaration = IdentityHashMap<CfirDeclaration, MutableSet<Long>>()
        for (capturedSource in constructionInputs.declarationReplacementSources) {
            val replacement = capturedSource.replacement
            check(originSurface(replacement.surfaceId) != null) {
                "A published declaration provenance must refer to a registered macro surface."
            }
            replacement.replacements.forEach { declaration ->
                check(declarationContainers.containsKey(declaration)) {
                    "A declaration provenance output must belong to the published semantic snapshot."
                }
                directSurfaceIdsByDeclaration.getOrPut(declaration, ::linkedSetOf) += replacement.surfaceId
            }
        }

        val expressionSurfaceIds = IdentityHashMap<CfirExpression, MutableSet<Long>>()
        for (capturedSource in constructionInputs.expressionReplacementSources) {
            val replacement = capturedSource.replacement
            check(originSurface(replacement.surfaceId) != null) {
                "A published expression provenance must refer to a registered macro surface."
            }
            expressionSurfaceIds.getOrPut(replacement.replacement, ::linkedSetOf) += replacement.surfaceId
        }

        val generatedSurfaceIdsByDeclaration = IdentityHashMap<CfirDeclaration, MutableSet<Long>>()
        for (file in files) {
            file.accept(object : CfirDefaultVisitor<Unit, Set<Long>>() {
                override fun visitElement(element: CfirElement, inheritedSurfaceIds: Set<Long>) {
                    ProgressManager.checkCanceled()
                    val directSurfaceIds = when (element) {
                        is CfirDeclaration -> directSurfaceIdsByDeclaration[element]
                        is CfirExpression -> expressionSurfaceIds[element]
                        else -> null
                    }
                    val currentSurfaceIds = directSurfaceIds ?: inheritedSurfaceIds
                    if (element is CfirDeclaration && currentSurfaceIds.isNotEmpty()) {
                        generatedSurfaceIdsByDeclaration.getOrPut(element, ::linkedSetOf).addAll(currentSurfaceIds)
                    }
                    element.acceptChildren(this, currentSurfaceIds)
                }
            }, emptySet())
        }

        filesByPackage = freezeLists(packageFiles)
        classifiersByClassId = freezeLists(classifiers)
        declarationsBySourceIdentity = freezeIdentityLists(declarationsBySource)
        classifiersBySourceIdentity = freezeIdentityLists(classifiersBySource)
        physicalSourceByDeclarationIdentity = freezeIdentityLists(physicalSourceByDeclaration)
        declarationReplacementCarriers = Collections.unmodifiableSet(replacementCarriers)
        expressionsByMacroSourceIdentity = Collections.unmodifiableMap(expressionsByMacroSource)
        expressionsBySourceFileIdentity = freezeIdentityMaps(expressionsBySourceFile)
        callablesById = freezeLists(callables)
        classifierNamesByPackage = freezeSets(classifierNames)
        callableNamesByPackage = freezeSets(callableNames)
        macroGeneratedClassifierNamesByPackage = freezeSets(macroGeneratedClassifierNames)
        macroGeneratedCallableNamesByPackage = freezeSets(macroGeneratedCallableNames)
        containerByDeclaration = Collections.unmodifiableMap(declarationContainers)
        macroSurfaceIdsByDeclarationIdentity = Collections.unmodifiableMap(
            IdentityHashMap<CfirDeclaration, Set<Long>>().also { frozen ->
                generatedSurfaceIdsByDeclaration.forEach { (declaration, surfaceIds) ->
                    frozen[declaration] = Collections.unmodifiableSet(surfaceIds.toSet())
                }
            },
        )
    }

    /** 返回包内最终语义文件；快照发布后结果可精确为空。 */
    fun filesInPackage(packageFqName: FqName): List<CfirFile> = filesByPackage[packageFqName].orEmpty()

    /** 返回 class id 对应的最终 class-like 声明。 */
    fun classifiers(classId: ClassId): List<CfirClassLikeDeclaration> = classifiersByClassId[classId].orEmpty()

    /** 通过物理 PSI 对象身份返回最终 class-like 输出；保留一对多结果，不按名称猜选。 */
    fun classifiersForSource(source: CjClassLikeDeclaration): List<CfirClassLikeDeclaration> =
        classifiersBySourceIdentity[source].orEmpty()

    /** 仅当物理 class-like 声明在当前快照中只有一个最终 class-like 输出时返回该声明。 */
    fun classifierForSource(source: CjClassLikeDeclaration): CfirClassLikeDeclaration? =
        classifiersForSource(source).singleOrNull()

    /** 按精确 PSI 身份返回最终语义声明；多个结果时调用方必须显式处理基数。 */
    fun declarationsForSource(source: CjDeclaration): List<CfirDeclaration> =
        declarationsBySourceIdentity[source].orEmpty()

    /** 唯一语义声明查询；一对多或无输出均返回 null。 */
    fun declarationForSource(source: CjDeclaration): CfirDeclaration? =
        declarationsForSource(source).singleOrNull()

    /** 判断声明是否是 splice 注册的物理 carrier；空结果 carrier 也会返回 true。 */
    fun isDeclarationReplacementCarrier(source: CjDeclaration): Boolean = source in declarationReplacementCarriers

    /** 通过精确 PSI 身份返回表达式宏调用在最终语义树中的展开表达式。 */
    fun expressionForMacroSource(source: CjMacroExpression): CfirExpression? =
        expressionsByMacroSourceIdentity[source]

    /** 返回指定物理文件中实际 splice 的表达式宏调用映射。 */
    fun expressionReplacementsForSource(sourceFile: CjFile): Map<CjMacroExpression, CfirExpression> =
        expressionsBySourceFileIdentity[sourceFile].orEmpty()

    /** 返回由物理 class-like carrier 唯一映射来的源声明，用于结构树的诊断所有权。 */
    fun physicalSourceForClassifier(
        classifier: CfirClassLikeDeclaration,
        sourceFile: CjFile,
    ): CjClassLikeDeclaration? = physicalSourceForDeclaration(classifier, sourceFile) as? CjClassLikeDeclaration

    /** 返回唯一拥有独立声明结构的物理 carrier；一对多输出由文件根结构统一拥有。 */
    fun physicalSourceForDeclaration(
        declaration: CfirDeclaration,
        sourceFile: CjFile,
    ): CjDeclaration? {
        val sourceSemanticFile = filesBySourceIdentity[sourceFile] ?: return null
        if (containerByDeclaration[declaration] !== sourceSemanticFile) return null
        val physicalSource = uniquePhysicalSourceForDeclaration(declaration) ?: return null
        if (physicalSource.sourceFile !== sourceFile) return null
        return physicalSource.sourcePsi
    }

    /** 判断该输出是否由唯一物理 carrier 产生，供嵌套声明结构分配唯一诊断 owner。 */
    fun hasUniquePhysicalSourceForDeclaration(
        declaration: CfirDeclaration,
        semanticFile: CfirFile,
    ): Boolean {
        if (containerByDeclaration[declaration] !== semanticFile) return false
        val physicalSource = uniquePhysicalSourceForDeclaration(declaration) ?: return false
        return filesBySourceIdentity[physicalSource.sourceFile] === semanticFile
    }

    /** 判断输出声明是否由指定语义文件内的物理 replacement carrier 产生。 */
    fun isDeclarationReplacementOutput(
        declaration: CfirDeclaration,
        semanticFile: CfirFile,
    ): Boolean = physicalSourceByDeclarationIdentity[declaration].orEmpty().any { source ->
        filesBySourceIdentity[source.sourceFile] === semanticFile
    }

    private fun uniquePhysicalSourceForDeclaration(declaration: CfirDeclaration): PhysicalDeclarationSource? {
        val physicalSource = physicalSourceByDeclarationIdentity[declaration].orEmpty().singleOrNull() ?: return null
        if (declarationsForSource(physicalSource.sourcePsi).singleOrNull() !== declaration) return null
        return physicalSource
    }

    /** 判断声明是否来自指定文件中的宏替换，包含一对多输出。 */
    fun isDeclarationReplacementOutput(declaration: CfirDeclaration, sourceFile: CjFile): Boolean =
        physicalSourceByDeclarationIdentity[declaration].orEmpty().any { it.sourceFile === sourceFile }

    /** 返回顶层 callable id 对应的最终声明。 */
    fun callables(callableId: CallableId): List<CfirCallableDeclaration> = callablesById[callableId].orEmpty()

    /** 返回包内顶层 classifier 名称。 */
    fun classifierNames(packageFqName: FqName): Set<Name> = classifierNamesByPackage[packageFqName].orEmpty()

    /** 返回包内顶层 callable 名称。 */
    fun callableNames(packageFqName: FqName): Set<Name> = callableNamesByPackage[packageFqName].orEmpty()

    /** 返回包内经宏来源映射确认生成的顶层 class-like 名称。 */
    fun macroGeneratedClassifierNames(packageFqName: FqName): Set<Name> =
        macroGeneratedClassifierNamesByPackage[packageFqName].orEmpty()

    /** 返回包内经宏来源映射确认生成的顶层 callable 名称。 */
    fun macroGeneratedCallableNames(packageFqName: FqName): Set<Name> =
        macroGeneratedCallableNamesByPackage[packageFqName].orEmpty()

    /** 返回某物理 source file 对应的最终语义 CFIR。 */
    fun fileForSource(sourceFile: CjFile): CfirFile? = filesBySourceIdentity[sourceFile]

    /** 返回声明所在的最终容器文件。 */
    fun containerFile(declaration: CfirDeclaration): CfirFile? = containerByDeclaration[declaration]

    /** 按最终 CFIR 对象身份返回声明的最近宏 surface 来源；多个来源表示歧义。 */
    fun macroSurfaceIdsForDeclaration(declaration: CfirDeclaration): Set<Long>? =
        macroSurfaceIdsByDeclarationIdentity[declaration]

    /** 查询发布时冻结的 surface，供声明导航使用稳定来源。 */
    fun originSurface(surfaceId: Long): MacroSurface? = originSurfacesById[surfaceId]

    /** 按发布时冻结的对象身份集合判断 CFIR source 是否由宏生成。 */
    fun isGeneratedSource(source: AbstractCjSourceElement): Boolean = source in generatedSourceIdentities

    private fun <K, V> freezeLists(source: Map<K, List<V>>): Map<K, List<V>> =
        Collections.unmodifiableMap(source.mapValues { (_, values) -> values.toList() })

    private fun <K, V> freezeSets(source: Map<K, MutableSet<V>>): Map<K, Set<V>> =
        Collections.unmodifiableMap(source.mapValues { (_, values) -> Collections.unmodifiableSet(values.toSet()) })

    private fun <K : Any, V : Any> freezeIdentityLists(source: IdentityHashMap<K, MutableList<V>>): Map<K, List<V>> =
        Collections.unmodifiableMap(IdentityHashMap<K, List<V>>().also { frozen ->
            source.forEach { (key, values) -> frozen[key] = values.toList() }
        })

    private fun freezeIdentityMaps(
        source: IdentityHashMap<CjFile, IdentityHashMap<CjMacroExpression, CfirExpression>>,
    ): Map<CjFile, Map<CjMacroExpression, CfirExpression>> =
        Collections.unmodifiableMap(IdentityHashMap<CjFile, Map<CjMacroExpression, CfirExpression>>().also { frozen ->
            source.forEach { (file, mappings) ->
                frozen[file] = Collections.unmodifiableMap(IdentityHashMap(mappings))
            }
        })

    private fun <T : Any> MutableList<T>.addIdentity(value: T) {
        if (none { it === value }) add(value)
    }

    private fun MutableList<PhysicalDeclarationSource>.addPhysicalSource(
        sourcePsi: CjDeclaration,
        sourceFile: CjFile,
    ) {
        if (none { it.sourcePsi === sourcePsi && it.sourceFile === sourceFile }) {
            add(PhysicalDeclarationSource(sourcePsi, sourceFile))
        }
    }
}

/**
 * 源码模块宏语义快照的原子发布点，同时作为 scope-session 缓存的修改跟踪器。
 */
class LLCfirMacroSemanticSnapshotStore : ModificationTracker {
    private data class State(val modificationCount: Long, val snapshot: LLCfirMacroSemanticSnapshot?)

    private val state = AtomicReference(State(0L, null))

    /** 当前完整语义快照；在发布成功前为 null。 */
    val snapshot: LLCfirMacroSemanticSnapshot?
        get() = state.get().snapshot

    override fun getModificationCount(): Long = state.get().modificationCount

    /** 在写访问之外构建索引；PSI 身份和物理源文件只在短读访问中捕获。 */
    internal fun prepareSnapshot(
        sourceFiles: List<CjFile>,
        files: List<CfirFile>,
        registry: MacroExpansionRegistry,
    ): LLCfirMacroSemanticSnapshot {
        val declarationsByFile = IdentityHashMap<CfirFile, List<CfirDeclaration>>()
        val fileByElement = IdentityHashMap<CfirElement, CfirFile>()
        for (file in files) {
            val declarations = mutableListOf<CfirDeclaration>()
            file.accept(object : CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    fileByElement[element] = file
                    if (element is CfirDeclaration) declarations += element
                    element.acceptChildren(this)
                }
            })
            declarationsByFile[file] = declarations.toList()
        }

        val sourceOriginsSnapshot = registry.snapshotGeneratedSourceOrigins(ProgressManager::checkCanceled)
        val generatedSourceOrigins = sourceOriginsSnapshot.originsBySource
        val resolvedOriginIds = sourceOriginsSnapshot.resolvedOriginIds
        val psiSources = ReadAction.compute<LLCfirMacroPsiSources, RuntimeException> {
            val declarationPsiByCfir = IdentityHashMap<CfirDeclaration, CjDeclaration?>()
            declarationsByFile.values.forEach { declarations ->
                declarations.forEach { declaration ->
                    ProgressManager.checkCanceled()
                    declarationPsiByCfir[declaration] = declaration.psi as? CjDeclaration
                }
            }

            // PSI 源树身份和文本范围只在读访问中捕获；区间索引与查询留在读锁之外。
            val generatedSourceRanges = generatedSourceOrigins.mapNotNull { (abstractSource, origins) ->
                ProgressManager.checkCanceled()
                val source = abstractSource as? CjSourceElement ?: return@mapNotNull null
                val treeIdentity = sourceTreeIdentity(source) ?: return@mapNotNull null
                MacroGeneratedSourceRange(
                    sourceTreeIdentity = treeIdentity,
                    startOffset = source.startOffset,
                    endOffset = source.endOffset,
                    origins = origins,
                )
            }
            val macroGeneratedDeclarationSources = buildList {
                files.forEach { file ->
                    file.declarations.forEach { declaration ->
                        ProgressManager.checkCanceled()
                        declaration.source?.let { source ->
                            val treeIdentity = (source as? CjSourceElement)?.let(::sourceTreeIdentity)
                            add(
                                MacroGeneratedDeclarationSource(
                                    declaration = declaration,
                                    sourceTreeIdentity = treeIdentity,
                                    startOffset = source.startOffset,
                                    endOffset = source.endOffset,
                                    exactOrigins = generatedSourceOrigins[source],
                                ),
                            )
                        }
                    }
                }
            }

            val declarationReplacementSources = registry.appliedDeclarationReplacements.mapNotNull { replacement ->
                ProgressManager.checkCanceled()
                val sourcePsi = replacement.carrier.psi as? CjDeclaration
                    ?: error("A declaration macro replacement carrier must retain its source declaration PSI.")
                val physicalSourceFile = if (sourcePsi.isPhysical) sourcePsi.containingFile as? CjFile else null
                LLCfirMacroDeclarationReplacementSource(replacement, sourcePsi, physicalSourceFile)
            }

            val expressionReplacementSources = registry.appliedExpressionReplacements.map { replacement ->
                ProgressManager.checkCanceled()
                check(replacement.carrier !== replacement.replacement) {
                    "A macro expression replacement must not reuse its construction-only carrier."
                }
                val surface = registry.originSurface(replacement.surfaceId)
                    as? org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
                    ?: error("A recorded expression replacement must refer to its original expression macro surface.")
                check(surface.replaceHandle.carrier === replacement.carrier) {
                    "A recorded expression replacement must use the carrier owned by its original macro surface."
                }
                val sourcePsi = replacement.carrier.psi as? CjMacroExpression
                    ?: error("A physical expression macro carrier must retain its exact CjMacroExpression PSI.")
                val surfaceSourcePsi = (surface.sourceRange?.source as? CjPsiSourceElement)?.psi
                check(surfaceSourcePsi == null || surfaceSourcePsi === sourcePsi) {
                    "A macro expression surface source must be the same PSI as its stable-splice carrier."
                }
                check(sourcePsi.isPhysical) {
                    "A published expression macro replacement must map to physical source PSI."
                }
                val sourceFile = sourcePsi.containingFile as? CjFile
                    ?: error("A physical expression macro call must belong to a CangJie source file.")
                LLCfirMacroExpressionReplacementSource(replacement, sourcePsi, sourceFile)
            }

            LLCfirMacroPsiSources(
                declarationPsiByCfir = declarationPsiByCfir,
                generatedSourceRanges = generatedSourceRanges,
                macroGeneratedDeclarationSources = macroGeneratedDeclarationSources,
                declarationReplacementSources = declarationReplacementSources,
                expressionReplacementSources = expressionReplacementSources,
            )
        }

        val sourceIndex = MacroGeneratedSourceIndex(psiSources.generatedSourceRanges, resolvedOriginIds)
        val macroGeneratedDeclarations = Collections.newSetFromMap(
            IdentityHashMap<CfirDeclaration, Boolean>(),
        )
        psiSources.macroGeneratedDeclarationSources.forEach { declarationSource ->
            ProgressManager.checkCanceled()
            if (sourceIndex.hasUniqueOrigin(declarationSource)) {
                macroGeneratedDeclarations += declarationSource.declaration
            }
        }
        val constructionInputs = LLCfirMacroSemanticSnapshotConstructionInputs(
            declarationsByFile = declarationsByFile,
            fileByElement = fileByElement,
            declarationPsiByCfir = psiSources.declarationPsiByCfir,
            macroGeneratedDeclarations = macroGeneratedDeclarations,
            declarationReplacementSources = psiSources.declarationReplacementSources,
            expressionReplacementSources = psiSources.expressionReplacementSources,
        )
        return LLCfirMacroSemanticSnapshot(sourceFiles, files, registry, constructionInputs)
    }

    /** 原子发布已经预构建完成的源码文件、expanded CFIR 与映射索引。 */
    @Synchronized
    internal fun publish(snapshot: LLCfirMacroSemanticSnapshot): Boolean {
        val current = state.get()
        if (current.snapshot != null) return false
        state.set(State(current.modificationCount + 1, snapshot))
        return true
    }

    private data class LLCfirMacroPsiSources(
        val declarationPsiByCfir: IdentityHashMap<CfirDeclaration, CjDeclaration?>,
        val generatedSourceRanges: List<MacroGeneratedSourceRange>,
        val macroGeneratedDeclarationSources: List<MacroGeneratedDeclarationSource>,
        val declarationReplacementSources: List<LLCfirMacroDeclarationReplacementSource>,
        val expressionReplacementSources: List<LLCfirMacroExpressionReplacementSource>,
    )
}
