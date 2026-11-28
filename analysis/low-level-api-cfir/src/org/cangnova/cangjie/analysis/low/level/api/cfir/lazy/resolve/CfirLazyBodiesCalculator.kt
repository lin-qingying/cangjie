

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import com.intellij.psi.PsiElement
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirResolvableSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.PreservedState
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.StateKeeper
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.resolveWithKeeper
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.getContainingFile
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirErrorPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFinalizer
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.parserSettings
import org.cangnova.cangjie.cfir.resolve.providers.macro.materializeDefaultParameterHelperBodies
import org.cangnova.cangjie.cfir.resolve.providers.macro.prepareDefaultParameterFunctionsInRebuiltBody
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.rawMacroSurfaceUsageStoreOrNull
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirRawMacroUsageSlot
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.source.psi as sourcePsi
import org.jetbrains.annotations.TestOnly

/**
 * 负责把低阶 CFIR 中的惰性 body、惰性 initializer 与惰性注解参数替换为真实 CFIR 子树。
 *
 * 该对象只处理已经由 lazy resolve designation 精确定位到的非局部声明。重建时会用当前声明的 PSI
 * 重新构造对应 raw CFIR，再把原声明中的惰性占位替换为重建结果，保持符号和外层 declaration path 不变。
 */
@LLCfirInternals
object CfirLazyBodiesCalculator {
    /**
     * 计算 [designation] 指向声明中的惰性 body 或 initializer。
     *
     * 只会沿 [CfirDesignation.path] 进入目标声明，避免遍历无关非局部声明。
     */
    internal fun calculateBodies(designation: CfirDesignation, publications: CfirLazyBodyPublications) {
        designation.target.transformSingle(
            CfirTargetLazyBodiesCalculatorTransformer(publications),
            designation.path.toPersistentList(),
        )
    }

    /**
     * 测试专用入口：计算 [cfirFile] 中所有惰性注解参数、body 与 initializer。
     *
     * 该入口递归处理整个文件，适合在测试中强制展开所有惰性节点以验证 raw rebuild 结果。
     */
    @TestOnly
    fun calculateAllLazyExpressionsInFile(cfirFile: CfirFile) {
        val session = cfirFile.moduleData.session
        // 此 LL 入口处理已缓存/注册的源图；不能隐式发布直接构建的私有 raw 文件。
        val components = checkNotNull(cfirFile.llCfirResolvableSession).moduleComponents
        val sourcePsi = checkNotNull(cfirFile.psi as? CjFile)
        check(components.cache.getCachedCfirFile(sourcePsi) === cfirFile) {
            "All-lazy source materialization requires the actual cached file generation."
        }
        checkNotNull(session.rawMacroSurfaceUsageStoreOrNull).validateAdoptionSlots(cfirFile, emptyList())
        CfirLazyBodyPublications(session).use { publications ->
            val graphKeeper = StateKeeper<CfirFile, CfirDesignation> { file, _ -> preserveRawGraph(file) }
            resolveWithKeeper(
                cfirFile,
                CfirDesignation(cfirFile),
                publications.withStateKeeper(graphKeeper),
                prepareTarget = {
                    cfirFile.accept(RecursiveLazyAnnotationCalculatorVisitor, session)
                    cfirFile.transformSingle(CfirAllLazyBodiesCalculatorTransformer(publications), persistentListOf())
                },
            ) {}
        }
    }

    /**
     * 计算 [cfirElement] 上非 body 区域中的惰性注解调用参数。
     */
    fun calculateAnnotations(cfirElement: CfirElementWithResolveState) {
        cfirElement.accept(LazyAnnotationCalculatorVisitor, cfirElement.moduleData.session)
    }

    /**
     * 计算单个 [annotationCall] 的惰性实参列表。
     */
    fun calculateAnnotation(annotationCall: CfirAnnotationCall, session: CfirSession) {
        calculateAnnotationCallIfNeeded(annotationCall, session)
    }

    /**
     * 为 [annotationCall] 提供可用于替换惰性 annotation 参数的参数列表。
     *
     * 仓颉 annotation 的 argument list 已由 raw CFIR 或 metadata 承载；低阶阶段不能在缺失 PSI 时重建
     * annotation，因此这里返回现有 CFIR 参数列表作为唯一可信入口。
     */
    fun createArgumentsForAnnotation(annotationCall: CfirAnnotationCall, session: CfirSession): CfirArgumentList {
        /*
         * v5.14 custom annotation 闭环后，annotation slot 由 raw CFIR 和 metadata
         * 共同承载。LL/lazy 阶段不能在 `psi == null` 时重建 PSI annotation；
         * 已有的 CFIR argumentList 是唯一入口。
         */
        return annotationCall.argumentList
    }

    /**
     * 判断 [cfirAnnotationCall] 的实参列表中是否仍包含惰性表达式。
     */
    fun needCalculatingAnnotationCall(cfirAnnotationCall: CfirAnnotationCall): Boolean =
        cfirAnnotationCall.argumentList.arguments.any { it is CfirLazyExpression }
}

/**
 * 通过目标声明的 PSI 重新构建 raw CFIR，并取回类型为 [T] 的重建声明。
 *
 * [psi] 默认来自 designation target；fake override 或 delegated 形态需要调用方传入 [originalPsi]。
 */
private inline fun <reified T : CfirDeclaration> revive(
    designation: CfirDesignation,
    annotationMetadata: CfirAnnotationMetadataBuilder,
    macroSurfaces: MutableList<MacroSurface>,
    psi: PsiElement? = designation.target.psi,
): T {
    val session = designation.target.moduleData.session
    val rootNonLocalDeclaration = psi as? CjElement
        ?: errorWithCfirSpecificEntries(
            "PSI is not available for lazy body reconstruction",
            cfir = designation.target,
            psi = psi,
        )

    val recreatedDeclaration = RawCfirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
        session = session,
        scopeProvider = session.cangjieScopeProvider,
        designation = designation,
        rootNonLocalDeclaration = rootNonLocalDeclaration,
        annotationMetadata = annotationMetadata,
        macroSurfaces = macroSurfaces,
    ) as T
    recreateMacroExpandedExpressions(session, rootNonLocalDeclaration, recreatedDeclaration)
    return recreatedDeclaration
}

/**
 * 把物理 PSI raw rebuild 中重新生成的宏错误 carrier 替换为当前语义快照记录的 CFIR 节点。
 *
 * 宏快照中的展开函数体在 lazy resolve 前会被状态保持器暂时换成 lazy placeholder；
 * 从物理 PSI revive 时必须恢复同一代展开结果，否则解析器会把宏调用重新当作 raw error carrier。
 * replacement 以精确宏调用 PSI 身份匹配，并复用快照中的 CFIR 节点身份，使文件结构映射、部分解析和展开结果保持一致。
 */
private fun recreateMacroExpandedExpressions(
    session: CfirSession,
    rootPsi: CjElement,
    recreatedDeclaration: CfirDeclaration,
) {
    val snapshot = (session as? LLCfirResolvableModuleSession)?.macroSemanticSnapshot ?: return
    val expressionReplacements = snapshot.expressionReplacementsForSource(rootPsi.containingCjFile)
    if (expressionReplacements.isEmpty()) return

    val transformer = object : CfirDefaultTransformer<Unit>() {
        override fun transformErrorExpression(
            errorExpression: CfirErrorExpression,
            data: Unit,
        ): CfirExpression {
            val macroSource = errorExpression.source?.sourcePsi as? CjMacroExpression
                ?: return super.transformErrorExpression(errorExpression, data)
            return expressionReplacements[macroSource]
                ?: super.transformErrorExpression(errorExpression, data)
        }
    }
    recreatedDeclaration.transformSingle(transformer, Unit)
}

/** 所有结构与归属校验完成后才采用；OPEN metadata 由本次 publications 持有到根 postProcess。 */
private class RebuiltDeclarationPlan(val slots: List<CfirRawMacroUsageSlot>, val adopt: () -> Unit)

private inline fun <reified T : CfirDeclaration> adoptRebuiltDeclaration(
    designation: CfirDesignation,
    psi: PsiElement?,
    publications: CfirLazyBodyPublications,
    prepare: (T, CfirAnnotationMetadataBuilder, CfirFile) -> RebuiltDeclarationPlan,
) {
    val target = designation.target as CfirDeclaration
    val session = target.moduleData.session
    // IGNORE_SELF 的祖先只提供解析上下文，产品仍使用 target 的实际文件与 session。
    val sourceFile = checkNotNull(target.getContainingFile()) {
        "A lazy body publication requires the target's actual cached source file."
    }
    check(sourceFile.moduleData.session === session) {
        "A lazy body publication cannot cross its source-file session."
    }
    val metadata = CfirAnnotationMetadataBuilder()
    val surfaces = mutableListOf<MacroSurface>()
    var transferred = false
    try {
        val copy = revive<T>(designation, metadata, surfaces, psi)
        val plan = prepare(copy, metadata, sourceFile)
        publications.stage(target, sourceFile, metadata, plan.slots, surfaces)
        transferred = true
        plan.adopt()
    } finally {
        if (!transferred) metadata.abort()
    }
}

/** 正文/default 槽位的完整结构校验在任何采用之前执行。 */
private fun functionReplacementSlots(target: CfirFunction, copy: CfirFunction): List<CfirRawMacroUsageSlot> {
    require(target.valueParameters.size == copy.valueParameters.size)
    return buildList {
        if (target.body is CfirLazyBlock) {
            require(copy.body !is CfirLazyBlock) { "A normal rebuild must materialize the function body." }
            add(CfirRawMacroUsageSlot(target, CfirRawMacroUsageSlot.Kind.BODY))
        }
        target.valueParameters.zip(copy.valueParameters).forEach { (original, recreated) ->
            if (original.defaultValue is CfirLazyExpression) {
                require(recreated.defaultValue !is CfirLazyExpression) { "A normal rebuild must materialize parameter defaults." }
                add(CfirRawMacroUsageSlot(original, CfirRawMacroUsageSlot.Kind.PARAMETER_DEFAULT))
            }
        }
    }
}

/** 参数数量已经由准备阶段校验，采用时只写入仍为 lazy 的实际参数。 */
private fun replaceLazyValueParameters(target: CfirFunction, copy: CfirFunction) {
    for ((valueParameter, newValueParameter) in target.valueParameters.zip(copy.valueParameters)) {
        if (valueParameter.defaultValue is CfirLazyExpression) valueParameter.replaceDefaultValue(newValueParameter.defaultValue)
    }
}

/** 用重建结果填充正文槽位，并核对 return 仍绑定到采用正文的真实宿主。 */
private fun replaceLazyBody(target: CfirFunction, copy: CfirFunction) {
    if (target.body !is CfirLazyBlock) return
    val body = copy.body
    body?.acceptChildren(object : CfirVisitorVoid() {
        override fun visitReturnExpression(returnExpression: CfirReturnExpression) {
            // raw rebuild 会优先复用真实函数目标，仍绑定副本的 target 只在采用该副本时迁移。
            val returnTarget = returnExpression.target
            if (returnTarget.isBound && returnTarget.labeledElement === copy) {
                returnTarget.bind(target)
            }
            returnExpression.acceptChildren(this)
        }

        override fun visitElement(element: CfirElement) {
            element.acceptChildren(this)
        }
    })
    target.replaceBody(body)
}

private val CfirCallableDeclaration.originalPsi: PsiElement?
    get() = unwrapFakeOverridesOrDelegated().psi

private inline fun <reified F : CfirFunction> calculateLazyBodiesForFunction(
    designation: CfirDesignation,
    publications: CfirLazyBodyPublications,
) {
    val function = designation.target as F
    require(needCalculatingLazyBodyForFunction(function))
    adoptRebuiltDeclaration<F>(designation, function.originalPsi, publications) { copy, metadata, sourceFile ->
        copy.body?.let { prepareDefaultParameterFunctionsInRebuiltBody(it, metadata, checkNotNull(sourceFile.parserSettings).backend) }
        val helperBodies = materializeDefaultParameterHelperBodies(function, copy, metadata, checkNotNull(sourceFile.parserSettings).backend)
        val slots = functionReplacementSlots(function, copy) + helperBodies.map { (helper, _) ->
            CfirRawMacroUsageSlot(helper, CfirRawMacroUsageSlot.Kind.BODY)
        }
        RebuiltDeclarationPlan(slots) {
            replaceLazyBody(function, copy)
            replaceLazyValueParameters(function, copy)
            helperBodies.forEach { (helper, body) -> helper.replaceBody(body) }
        }
    }
}

private fun calculateLazyBodyForConstructor(designation: CfirDesignation, publications: CfirLazyBodyPublications) {
    val constructor = designation.target as CfirConstructor
    require(needCalculatingLazyBodyForConstructor(constructor))
    adoptRebuiltDeclaration<CfirConstructor>(designation, constructor.originalPsi, publications) { copy, metadata, sourceFile ->
        copy.body?.let { prepareDefaultParameterFunctionsInRebuiltBody(it, metadata, checkNotNull(sourceFile.parserSettings).backend) }
        val helperBodies = materializeDefaultParameterHelperBodies(constructor, copy, metadata, checkNotNull(sourceFile.parserSettings).backend)
        val slots = functionReplacementSlots(constructor, copy) + helperBodies.map { (helper, _) ->
            CfirRawMacroUsageSlot(helper, CfirRawMacroUsageSlot.Kind.BODY)
        }
        RebuiltDeclarationPlan(slots) {
            replaceLazyBody(constructor, copy)
            replaceLazyValueParameters(constructor, copy)
            helperBodies.forEach { (helper, body) -> helper.replaceBody(body) }
        }
    }
}

private fun calculateLazyBodyForProperty(designation: CfirDesignation, publications: CfirLazyBodyPublications) {
    val property = designation.target as CfirProperty
    if (!needCalculatingLazyBodyForProperty(property)) return
    adoptRebuiltDeclaration<CfirProperty>(designation, property.originalPsi, publications) { copy, metadata, sourceFile ->
        val pairs = listOfNotNull(
            property.getter?.let { target ->
                target to (copy.getter ?: errorWithCfirSpecificEntries("Recreated getter is missing", cfir = copy, psi = copy.psi))
            },
            property.setter?.let { target ->
                target to (copy.setter ?: errorWithCfirSpecificEntries("Recreated setter is missing", cfir = copy, psi = copy.psi))
            },
        )
        val helperBodies = pairs.flatMap { (target, recreated) ->
            recreated.body?.let { prepareDefaultParameterFunctionsInRebuiltBody(it, metadata, checkNotNull(sourceFile.parserSettings).backend) }
            materializeDefaultParameterHelperBodies(target, recreated, metadata, checkNotNull(sourceFile.parserSettings).backend)
        }
        val slots = pairs.flatMap { (target, recreated) -> functionReplacementSlots(target, recreated) } + helperBodies.map { (helper, _) ->
            CfirRawMacroUsageSlot(helper, CfirRawMacroUsageSlot.Kind.BODY)
        }
        RebuiltDeclarationPlan(slots) {
            pairs.forEach { (target, recreated) ->
                replaceLazyBody(target, recreated)
                replaceLazyValueParameters(target, recreated)
            }
            helperBodies.forEach { (helper, body) -> helper.replaceBody(body) }
        }
    }
}

private inline fun <reified V : CfirVariable> calculateLazyInitializerForVariable(
    designation: CfirDesignation,
    publications: CfirLazyBodyPublications,
) {
    val variable = designation.target as V
    require(needCalculatingLazyInitializerForVariable(variable))
    adoptRebuiltDeclaration<V>(designation, variable.originalPsi, publications) { copy, _, _ ->
        require(copy.initializer !is CfirLazyExpression) { "A normal rebuild must materialize the initializer." }
        RebuiltDeclarationPlan(listOf(CfirRawMacroUsageSlot(variable, CfirRawMacroUsageSlot.Kind.INITIALIZER))) {
            variable.replaceInitializer(copy.initializer)
        }
    }
}

/**
 * 判断构造函数是否仍有惰性 body 或惰性默认参数值需要展开。
 */
private fun needCalculatingLazyBodyForConstructor(constructor: CfirConstructor): Boolean =
    needCalculatingLazyBodyForFunction(constructor)

/**
 * 判断函数是否仍有惰性 body 或惰性默认参数值需要展开。
 */
private fun needCalculatingLazyBodyForFunction(function: CfirFunction): Boolean =
    function.body is CfirLazyBlock || function.valueParameters.any { it.defaultValue is CfirLazyExpression }

/**
 * 判断属性 getter 或 setter 中是否仍有惰性 body 或惰性默认参数值需要展开。
 */
private fun needCalculatingLazyBodyForProperty(property: CfirProperty): Boolean =
    property.getter?.let(::needCalculatingLazyBodyForFunction) == true ||
            property.setter?.let(::needCalculatingLazyBodyForFunction) == true

/**
 * 判断变量 initializer 是否仍需要通过 raw rebuild 重新计算。
 *
 * raw 构建使用 [CfirLazyExpression] 表示有初值但尚未构建的变量。
 */
private fun needCalculatingLazyInitializerForVariable(variable: CfirVariable): Boolean =
    variable.initializer is CfirLazyExpression

/**
 * 计算代码片段的惰性块。
 *
 * 代码片段以文件级片段 PSI 为根重建，随后只替换 [CfirCodeFragment.block]。
 */
private fun calculateLazyBodyForCodeFragment(designation: CfirDesignation, publications: CfirLazyBodyPublications) {
    val codeFragment = designation.target as CfirCodeFragment
    require(codeFragment.block is CfirLazyBlock)

    adoptRebuiltDeclaration<CfirCodeFragment>(designation, codeFragment.psi as? CjCodeFragment, publications) { copy, _, _ ->
        require(copy.block !is CfirLazyBlock)
        RebuiltDeclarationPlan(listOf(CfirRawMacroUsageSlot(codeFragment, CfirRawMacroUsageSlot.Kind.CODE_FRAGMENT))) {
            codeFragment.replaceBlock(copy.block)
        }
    }
}

/**
 * 递归计算文件或声明树中所有非 body 注解的惰性参数。
 */
private object RecursiveLazyAnnotationCalculatorVisitor : RecursiveNonLocalAnnotationVisitor<CfirSession>() {
    /**
     * 遇到注解时按需展开其中的惰性参数。
     */
    override fun processAnnotation(annotation: CfirAnnotation, data: CfirSession) {
        calculateAnnotationCallIfNeeded(annotation, data)
    }
}

/**
 * 仅计算目标非局部声明上的非 body 注解惰性参数。
 */
private object LazyAnnotationCalculatorVisitor : NonLocalAnnotationVisitor<CfirSession>() {
    /**
     * 遇到注解时按需展开其中的惰性参数。
     */
    override fun processAnnotation(annotation: CfirAnnotation, data: CfirSession) {
        calculateAnnotationCallIfNeeded(annotation, data)
    }
}

/**
 * 如果 [annotation] 是仍含惰性实参的 annotation call，则替换其参数列表。
 */
private fun calculateAnnotationCallIfNeeded(annotation: CfirAnnotation, session: CfirSession) {
    if (annotation !is CfirAnnotationCall || !CfirLazyBodiesCalculator.needCalculatingAnnotationCall(annotation)) return
    annotation.replaceArgumentList(CfirLazyBodiesCalculator.createArgumentsForAnnotation(annotation, session))
}

/** 在 test-only 全图入口首次修改前捕获所有实际引用；恢复覆盖 annotation arguments 和全部 lazy 槽位。 */
private fun preserveRawGraph(file: CfirFile): PreservedState {
    val restorations = mutableListOf<() -> Unit>()
    val visited = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirElement, Boolean>())
    val visitor = object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (!visited.add(element)) return
            when (element) {
                is CfirFunction -> {
                    val body = element.body
                    restorations += { element.replaceBody(body) }
                    element.valueParameters.forEach { parameter ->
                        val defaultValue = parameter.defaultValue
                        restorations += { parameter.replaceDefaultValue(defaultValue) }
                    }
                }
                is CfirCodeFragment -> {
                    val block = element.block
                    restorations += { element.replaceBlock(block) }
                }
                is CfirVariable -> {
                    val initializer = element.initializer
                    restorations += { element.replaceInitializer(initializer) }
                }
                is CfirAnnotationCall -> {
                    val arguments = element.argumentList
                    restorations += { element.replaceArgumentList(arguments) }
                }
            }
            element.acceptChildren(this)
        }
    }
    file.accept(visitor)
    return object : PreservedState {
        private var restored = false
        override fun postProcess() {}
        override fun restore() {
            if (restored) return
            restored = true
            restorations.asReversed().forEach { it() }
        }
    }
}

/**
 * 测试场景使用的全量惰性 body 计算 transformer。
 */
private class CfirAllLazyBodiesCalculatorTransformer(publications: CfirLazyBodyPublications) :
    CfirLazyBodiesCalculatorTransformer(publications) {
    /**
     * 对文件、类和扩展节点递归进入子声明，其余节点交给基类按目标类型处理。
     */
    override fun <E : CfirElement> transformElement(element: E, data: PersistentList<CfirDeclaration>): E {
        return recursiveTransformation(element, data)
    }
}

/**
 * 只处理 designation 指定目标声明的惰性 body 计算 transformer。
 */
private class CfirTargetLazyBodiesCalculatorTransformer(publications: CfirLazyBodyPublications) :
    CfirLazyBodiesCalculatorTransformer(publications)

/**
 * 根据 CFIR 声明种类展开对应惰性 body、initializer 或代码片段块的 transformer 基类。
 */
private sealed class CfirLazyBodiesCalculatorTransformer(protected val publications: CfirLazyBodyPublications) :
    CfirTransformer<PersistentList<CfirDeclaration>>() {
    /**
     * 默认不处理普通元素，避免遍历目标 designation 之外的节点。
     */
    override fun <E : CfirElement> transformElement(element: E, data: PersistentList<CfirDeclaration>): E = element

    /**
     * 计算命名函数的惰性函数体和默认参数值。
     */
    override fun transformNamedFunction(
        namedFunction: CfirNamedFunction,
        data: PersistentList<CfirDeclaration>,
    ): CfirNamedFunction {
        if (needCalculatingLazyBodyForFunction(namedFunction)) {
            calculateLazyBodiesForFunction<CfirNamedFunction>(CfirDesignation(data, namedFunction), publications)
        }
        return namedFunction
    }

    /**
     * 计算入口函数的惰性函数体和默认参数值。
     */
    override fun transformMainFunction(
        mainFunction: CfirMainFunction,
        data: PersistentList<CfirDeclaration>,
    ): CfirMainFunction {
        if (needCalculatingLazyBodyForFunction(mainFunction)) {
            calculateLazyBodiesForFunction<CfirMainFunction>(CfirDesignation(data, mainFunction), publications)
        }
        return mainFunction
    }

    /**
     * 计算宏声明函数体中的惰性 body 和默认参数值。
     */
    override fun transformMacroDeclaration(
        macroDeclaration: CfirMacroDeclaration,
        data: PersistentList<CfirDeclaration>,
    ): CfirMacroDeclaration {
        if (needCalculatingLazyBodyForFunction(macroDeclaration)) {
            calculateLazyBodiesForFunction<CfirMacroDeclaration>(CfirDesignation(data, macroDeclaration), publications)
        }
        return macroDeclaration
    }

    /**
     * 计算 finalizer 的惰性函数体。
     */
    override fun transformFinalizer(
        finalizer: CfirFinalizer,
        data: PersistentList<CfirDeclaration>,
    ): CfirFinalizer {
        if (needCalculatingLazyBodyForFunction(finalizer)) {
            calculateLazyBodiesForFunction<CfirFinalizer>(CfirDesignation(data, finalizer), publications)
        }
        return finalizer
    }

    /**
     * 计算构造函数的惰性函数体和默认参数值。
     */
    override fun transformConstructor(
        constructor: CfirConstructor,
        data: PersistentList<CfirDeclaration>,
    ): CfirConstructor {
        if (needCalculatingLazyBodyForConstructor(constructor)) {
            calculateLazyBodyForConstructor(CfirDesignation(data, constructor), publications)
        }
        return constructor
    }

    /**
     * 计算错误主构造函数的惰性函数体和默认参数值。
     */
    override fun transformErrorPrimaryConstructor(
        errorPrimaryConstructor: CfirErrorPrimaryConstructor,
        data: PersistentList<CfirDeclaration>,
    ): CfirErrorPrimaryConstructor {
        if (needCalculatingLazyBodyForConstructor(errorPrimaryConstructor)) {
            calculateLazyBodyForConstructor(CfirDesignation(data, errorPrimaryConstructor), publications)
        }
        return errorPrimaryConstructor
    }

    /**
     * 计算属性访问器中的惰性 getter/setter body。
     */
    override fun transformProperty(property: CfirProperty, data: PersistentList<CfirDeclaration>): CfirProperty {
        if (needCalculatingLazyBodyForProperty(property)) {
            calculateLazyBodyForProperty(CfirDesignation(data, property), publications)
        }
        return property
    }

    /**
     * 计算字段变量的惰性 initializer。
     */
    override fun transformFieldVariable(
        fieldVariable: CfirFieldVariable,
        data: PersistentList<CfirDeclaration>,
    ): CfirFieldVariable {
        if (needCalculatingLazyInitializerForVariable(fieldVariable)) {
            calculateLazyInitializerForVariable<CfirFieldVariable>(CfirDesignation(data, fieldVariable), publications)
        }
        return fieldVariable
    }

    /**
     * 计算模式变量的惰性 initializer。
     */
    override fun transformPatternVariable(
        patternVariable: CfirPatternVariable,
        data: PersistentList<CfirDeclaration>,
    ): CfirPatternVariable {
        if (needCalculatingLazyInitializerForVariable(patternVariable)) {
            calculateLazyInitializerForVariable<CfirPatternVariable>(CfirDesignation(data, patternVariable), publications)
        }
        return patternVariable
    }

    /**
     * 计算代码片段的惰性块。
     */
    override fun transformCodeFragment(
        codeFragment: CfirCodeFragment,
        data: PersistentList<CfirDeclaration>,
    ): CfirCodeFragment {
        if (codeFragment.block is CfirLazyBlock) {
            calculateLazyBodyForCodeFragment(CfirDesignation(data, codeFragment), publications)
        }
        return codeFragment
    }
}

/**
 * 对文件、类和扩展节点递归执行 [CfirTransformer]，并维护当前非局部 declaration path。
 */
private fun <E : CfirElement> CfirTransformer<PersistentList<CfirDeclaration>>.recursiveTransformation(
    element: E,
    data: PersistentList<CfirDeclaration>,
): E {
    if (element is CfirFile || element is CfirClass || element is CfirExtend) {
        val newList = data.add(element as CfirDeclaration)
        element.transformChildren(this, newList)
    }

    return element
}
