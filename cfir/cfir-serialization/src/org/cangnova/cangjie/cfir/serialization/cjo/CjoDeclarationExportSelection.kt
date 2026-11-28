package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.providers.CfirDeclarationExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysisSettings
import org.cangnova.cangjie.cfir.resolve.providers.exportAnalysisSettings
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.cfir.source.CfirSourceExpressionIdentity
import java.util.Collections
import java.util.IdentityHashMap

/** 本次 SOURCE 写出已选择的内容槽；根对象及 owner 冻结后供引用闭包和编码共同读取。 */
enum class CjoSourceContentSlot { FUNCTION_BODY, VARIABLE_INITIALIZER, PARAMETER_DEFAULT }

/** 原生函数正文开关属于实际保存阶段；预留声明索引不能取得 PRE_CONTENT 身份。 */
enum class CjoSourceSaveStage { PRE_CONTENT, FINAL }

/** Native SOURCE 保持固定 writer 合同；schema 保真使用显式合同，不冒称 cjc 已生产值池。 */
enum class CjoSourceEncodingContract { NATIVE_SOURCE, TYPED_SCHEMA }

data class CjoSourceContent(
    val owner: CfirDeclaration,
    val slot: CjoSourceContentSlot,
    val root: CfirSourceExpressionIdentity,
    val stage: CjoSourceSaveStage,
)

/** ASTWriter 的独立输入；COMMON owner 位不能冒充本次 serializingCommon。 */
data class CjoExportOptions(
    val exportForIncremental: Boolean = false,
    val exportContent: Boolean = true,
    val serializingCommon: Boolean = false,
    val isMacroPackage: Boolean = false,
    val analysisSettings: CfirExportAnalysisSettings = CfirExportAnalysisSettings(),
    val sourceEncodingContract: CjoSourceEncodingContract = CjoSourceEncodingContract.NATIVE_SOURCE,
) {
    val onlyVisibleSignatures: Boolean get() = !exportForIncremental && !exportContent

    companion object {
        /** 直接 CFIR 调用以实际模块角色确定 common；CLI 明确传入 invocation 的选项。 */
        fun forSourceFiles(files: Collection<CfirFile>, family: CfirCjmpFamilySnapshot?): CjoExportOptions {
            val modes = files.map { it.moduleData.session.cjmpSettings.mode }.distinct()
            require(family != null || modes.size == 1) { "A CJO invocation must have one compilation mode" }
            val settings = files.map { it.moduleData.session.exportAnalysisSettings }.distinct()
            require(settings.size == 1) { "A CJO invocation must have one export configuration" }
            val macroPackage = files.map { it.packageDirective.isMacroPackage }.distinct()
            require(macroPackage.size == 1) { "A CJO invocation must have one package kind" }
            return CjoExportOptions(
                serializingCommon = family == null && modes.single() == CfirCjmpMode.COMMON,
                isMacroPackage = macroPackage.single(),
                analysisSettings = settings.single(),
            )
        }
    }
}

/**
 * CJO 格式选择接口；声明定义／引用索引图与容器 body 分别选择。
 * 普通 private 可因真实引用取得 EXTERNAL linkage，语言 visibility 则继续保持 PRIVATE。
 */
interface CjoDeclarationExportSelection {
    fun shouldAlwaysExport(declaration: CfirDeclaration): Boolean
    fun shouldExportRoot(declaration: CfirDeclaration, collectedBeforeInstantiation: Boolean): Boolean
    fun shouldExportBodyMember(owner: CfirDeclaration, declaration: CfirDeclaration): Boolean
    /** 搜索候选与实际正文准入是两套原生判据，不能将前者当成已保存声明集合。 */
    fun isPreContentFunctionCandidate(function: CfirCallableDeclaration): Boolean
    /** 原生 partial FuncBody 同时省略参数表、返回类型和正文。 */
    fun isPartialFunction(function: CfirFunction): Boolean
    fun selectSourceContent(declaration: CfirDeclaration, stage: CjoSourceSaveStage): CjoSourceContent?
    fun sourceContent(declaration: CfirDeclaration): CjoSourceContent?
    fun sourceSaveStageOf(declaration: CfirDeclaration): CjoSourceSaveStage

    companion object {
        fun create(
            analysis: CfirDeclarationExportAnalysis,
            options: CjoExportOptions,
            isSuppressed: (CfirDeclaration) -> Boolean,
            isMatchedCommon: (CfirDeclaration) -> Boolean,
        ): CjoDeclarationExportSelection = NativeSelection(analysis, options, isSuppressed, isMatchedCommon)
    }
}

private class NativeSelection(
    private val analysis: CfirDeclarationExportAnalysis,
    private val options: CjoExportOptions,
    private val isSuppressed: (CfirDeclaration) -> Boolean,
    private val isMatchedCommon: (CfirDeclaration) -> Boolean,
) : CjoDeclarationExportSelection {
    private val sourceContents = IdentityHashMap<CfirDeclaration, CjoSourceContent?>()
    private val sourceSaveStages = IdentityHashMap<CfirDeclaration, CjoSourceSaveStage>()

    override fun isPreContentFunctionCandidate(function: CfirCallableDeclaration): Boolean {
        if (function is CfirEnumConstructor) return options.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA &&
            options.exportContent && !options.isMacroPackage && !isMatchedCommon(function) &&
            analysis.declarationTypeOf(function)?.containsErrorType() == false && analysis.hasCanonicalSourceOf(function) &&
            analysis.canonicalSourceGraphOf(function).functionBodies.containsKey(function)
        check(function is CfirFunction) { "PRE_CONTENT candidate must have a real callable F" }
        if (function is CfirAnonymousFunction || (!analysis.isSourceDeclaration(function) && !analysis.hasCanonicalSourceOf(function)) ||
            !options.exportContent || options.isMacroPackage || isMatchedCommon(function) ||
            analysis.declarationTypeOf(function)?.containsErrorType() != false) return false
        val status = analysis.statusOf(function)
        if (options.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA &&
            analysis.hasCanonicalSourceOf(function) && analysis.canonicalSourceGraphOf(function).functionBodies.containsKey(function)) return true
        return status.isConst || analysis.precedingFactsOf(function).isInline || analysis.isFrozenOf(function) ||
            status.isDefault && analysis.nativeOuterOf(function) is CfirInterface ||
            analysis.isGenericInCommonSerialization(function, options.serializingCommon)
    }

    /** 同一声明在首次实际保存时选择一次；FINAL 命中已保存记录时只复用原决定。 */
    override fun selectSourceContent(declaration: CfirDeclaration, stage: CjoSourceSaveStage): CjoSourceContent? {
        if (sourceContents.containsKey(declaration)) return sourceContents[declaration]
        require(stage != CjoSourceSaveStage.PRE_CONTENT || options.exportContent && !options.isMacroPackage) {
            "PRE_CONTENT is disabled for this CJO invocation"
        }
        val content = when {
            declaration is CfirValueParameter -> {
                val selected = if (analysis.isSourceDeclaration(declaration)) analysis.defaultValueOf(declaration)?.takeIf {
                    analysis.precedingFactsOf(declaration).isToolAdded || analysis.defaultAssignmentFactsOf(declaration).let { facts ->
                        facts.assignmentIsConst || facts.assignmentIsLiteral
                    }
                }?.let { analysis.canonicalSourceGraphOf(declaration).expressionOf(it) }
                else if (analysis.hasCanonicalSourceOf(declaration)) analysis.canonicalSourceGraphOf(declaration).defaults[declaration] else null
                selected?.let { CjoSourceContent(declaration, CjoSourceContentSlot.PARAMETER_DEFAULT, it, stage) }
            }
            declaration is CfirEnumConstructor -> if (options.exportContent && !isMatchedCommon(declaration) &&
                options.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA && stage == CjoSourceSaveStage.PRE_CONTENT &&
                analysis.hasCanonicalSourceOf(declaration) && analysis.declarationTypeOf(declaration)?.containsErrorType() == false)
                analysis.canonicalSourceGraphOf(declaration).functionBodies[declaration]
                    ?.let { CjoSourceContent(declaration, CjoSourceContentSlot.FUNCTION_BODY, it, stage) } else null
            !options.exportContent || isMatchedCommon(declaration) -> null
            declaration is CfirFunction && (stage != CjoSourceSaveStage.PRE_CONTENT ||
                !sourceFunctionBodyIsSelected(declaration) || analysis.declarationTypeOf(declaration)?.containsErrorType() != false) -> null
            declaration is CfirFunction -> {
                val available = if (analysis.isSourceDeclaration(declaration)) analysis.functionBodyOf(declaration)?.let {
                    analysis.canonicalSourceGraphOf(declaration).expressionOf(it)
                } else if (analysis.hasCanonicalSourceOf(declaration)) analysis.canonicalSourceGraphOf(declaration).functionBodies[declaration] else null
                available?.takeIf {
                    stage == CjoSourceSaveStage.PRE_CONTENT && sourceFunctionBodyIsSelected(declaration) &&
                        analysis.declarationTypeOf(declaration)?.containsErrorType() == false
                }?.let { CjoSourceContent(declaration, CjoSourceContentSlot.FUNCTION_BODY, it, stage) }
            }
            declaration is CfirVariable -> {
                if (!analysis.shouldExportSource(declaration)) {
                    sourceSaveStages[declaration] = stage
                    sourceContents[declaration] = null
                    return null
                }
                val available = if (analysis.isSourceDeclaration(declaration)) analysis.initializerOf(declaration)?.let {
                    analysis.canonicalSourceGraphOf(declaration).expressionOf(it)
                } else if (analysis.hasCanonicalSourceOf(declaration)) analysis.canonicalSourceGraphOf(declaration).initializers[declaration] else null
                available?.takeIf { analysis.shouldExportSource(declaration) }
                    ?.let { CjoSourceContent(declaration, CjoSourceContentSlot.VARIABLE_INITIALIZER, it, stage) }
            }
            else -> null
        }
        sourceSaveStages[declaration] = stage
        sourceContents[declaration] = content
        return content
    }

    override fun sourceContent(declaration: CfirDeclaration): CjoSourceContent? {
        check(sourceContents.containsKey(declaration)) {
            "Source content was queried before the declaration was saved: ${declaration.symbol}, origin=${declaration.origin}"
        }
        return sourceContents[declaration]
    }

    override fun sourceSaveStageOf(declaration: CfirDeclaration): CjoSourceSaveStage = sourceSaveStages.getValue(declaration)

    override fun isPartialFunction(function: CfirFunction): Boolean =
        analysis.precedingFactsOf(function).isGenericInstantiated && analysis.hasOwnGenericNode(function)

    private fun sourceFunctionBodyIsSelected(function: CfirFunction): Boolean {
        // Lambda 的 FuncBody 没有 native FuncDecl，不使用具名函数的资格及词法 frozen 传播。
        if (function is CfirAnonymousFunction) return true
        if (isPartialFunction(function)) return false
        if (options.sourceEncodingContract == CjoSourceEncodingContract.TYPED_SCHEMA &&
            analysis.hasCanonicalSourceOf(function) && analysis.canonicalSourceGraphOf(function).functionBodies.containsKey(function)) return true
        val frozen = writerFrozen(function)
        return analysis.sourceFunctionBodyIsEligible(function, frozen) ||
            analysis.isGenericInCommonSerialization(function, options.serializingCommon)
    }

    /** 原生 outerDecl 为函数时覆盖 frozen；ownerFunc 是另一条边，不能参与这项继承。 */
    private fun writerFrozen(function: CfirFunction): Boolean {
        var current = function
        val visited = Collections.newSetFromMap(IdentityHashMap<CfirFunction, Boolean>())
        while (true) {
            check(visited.add(current)) { "A function has cyclic lexical ownership" }
            val owner = analysis.nativeOuterOf(current) as? CfirFunction ?: return analysis.isFrozenOf(current)
            current = owner
        }
    }

    override fun shouldAlwaysExport(declaration: CfirDeclaration): Boolean = !isMatchedCommon(declaration) &&
        (analysis.isExported(declaration) || analysis.precedingFactsOf(declaration).isImplicitlyUsed ||
            analysis.isTopLevel(declaration) && analysis.linkageOf(declaration) != CfirDeclarationLinkage.INTERNAL ||
            options.serializingCommon && declaration is CfirExtend)

    override fun shouldExportRoot(declaration: CfirDeclaration, collectedBeforeInstantiation: Boolean): Boolean =
        !isSuppressed(declaration) && (options.exportForIncremental || shouldAlwaysExport(declaration) ||
            collectedBeforeInstantiation && analysis.linkageOf(declaration) != CfirDeclarationLinkage.INTERNAL ||
            options.serializingCommon && declaration !is CfirClassLikeDeclaration && analysis.isTopLevel(declaration))

    override fun shouldExportBodyMember(owner: CfirDeclaration, declaration: CfirDeclaration): Boolean {
        if (options.onlyVisibleSignatures && analysis.statusOf(owner).visibility == Visibilities.Private) return false
        if (isSuppressed(declaration) || declaration is CfirPatternVariable ||
            options.onlyVisibleSignatures && !analysis.isExported(declaration)) return false
        if (options.serializingCommon && owner is CfirExtend ||
            analysis.isGenericInCommonSerialization(declaration, options.serializingCommon)) return true
        val instanceVariable = declaration is CfirVariable && !analysis.statusOf(declaration).isStatic ||
            declaration is CfirEnumConstructor && analysis.parameterCountOf(declaration) == 0
        return options.exportForIncremental || analysis.statusOf(owner).isCommon ||
            instanceVariable || analysis.linkageOf(declaration) != CfirDeclarationLinkage.INTERNAL
    }
}
