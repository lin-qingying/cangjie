

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import com.intellij.psi.PsiElement
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
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
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjDeclarationWithInitializer
import org.cangnova.cangjie.psi.CjElement
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
    fun calculateBodies(designation: CfirDesignation) {
        designation.target.transformSingle(
            CfirTargetLazyBodiesCalculatorTransformer,
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
        cfirFile.accept(RecursiveLazyAnnotationCalculatorVisitor, cfirFile.moduleData.session)
        cfirFile.transformSingle(CfirAllLazyBodiesCalculatorTransformer, persistentListOf())
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
    psi: PsiElement? = designation.target.psi,
): T {
    val session = designation.target.moduleData.session
    val rootNonLocalDeclaration = psi as? CjElement
        ?: errorWithCfirSpecificEntries(
            "PSI is not available for lazy body reconstruction",
            cfir = designation.target,
            psi = psi,
        )

    return RawCfirNonLocalDeclarationBuilder.buildWithFunctionSymbolRebind(
        session = session,
        scopeProvider = session.cangjieScopeProvider,
        designation = designation,
        rootNonLocalDeclaration = rootNonLocalDeclaration,
    ) as T
}

/**
 * 用 [copy] 中的默认参数值替换 [target] 中仍为 [CfirLazyExpression] 的默认参数值。
 *
 * 参数数量必须保持一致，因为重建声明应与原声明拥有相同签名结构。
 */
private fun replaceLazyValueParameters(target: CfirFunction, copy: CfirFunction) {
    val targetParameters = target.valueParameters
    val copyParameters = copy.valueParameters
    require(targetParameters.size == copyParameters.size)

    for ((valueParameter, newValueParameter) in targetParameters.zip(copyParameters)) {
        if (valueParameter.defaultValue is CfirLazyExpression) {
            valueParameter.replaceDefaultValue(newValueParameter.defaultValue)
        }
    }
}

/**
 * 当 [target] 的 body 仍是 [CfirLazyBlock] 时，用 [copy] 的真实 body 替换它。
 */
private fun replaceLazyBody(target: CfirFunction, copy: CfirFunction) {
    if (target.body !is CfirLazyBlock) return
    target.replaceBody(copy.body)
}

/**
 * 返回 callable 的原始 PSI。
 *
 * fake override 或 delegated 声明需要先解包到真实声明，否则 raw rebuild 可能无法定位原始源码节点。
 */
private val CfirCallableDeclaration.originalPsi: PsiElement?
    get() = unwrapFakeOverridesOrDelegated().psi

/**
 * 计算普通函数类声明的惰性 body 和默认参数值。
 */
private inline fun <reified F : CfirFunction> calculateLazyBodiesForFunction(designation: CfirDesignation) {
    val function = designation.target as F
    require(needCalculatingLazyBodyForFunction(function))

    val recreatedFunction = revive<F>(designation, function.originalPsi)
    replaceLazyBody(function, recreatedFunction)
    replaceLazyValueParameters(function, recreatedFunction)
}

/**
 * 计算构造函数的惰性 body 和默认参数值。
 */
private fun calculateLazyBodyForConstructor(designation: CfirDesignation) {
    val constructor = designation.target as CfirConstructor
    require(needCalculatingLazyBodyForConstructor(constructor))

    val recreatedConstructor = revive<CfirConstructor>(designation, constructor.originalPsi)
    replaceLazyBody(constructor, recreatedConstructor)
    replaceLazyValueParameters(constructor, recreatedConstructor)
}

/**
 * 计算属性访问器中仍为惰性块的 getter/setter body。
 *
 * 属性本身没有需要替换的统一 body；需要分别处理已经存在的访问器，并要求重建结果中对应访问器仍存在。
 */
private fun calculateLazyBodyForProperty(designation: CfirDesignation) {
    val property = designation.target as CfirProperty
    if (!needCalculatingLazyBodyForProperty(property)) return

    val recreatedProperty = revive<CfirProperty>(designation, property.originalPsi)

    property.getter?.let { getter ->
        val recreatedGetter = recreatedProperty.getter
            ?: errorWithCfirSpecificEntries("Recreated getter is missing", cfir = recreatedProperty, psi = recreatedProperty.psi)
        replaceLazyBody(getter, recreatedGetter)
        replaceLazyValueParameters(getter, recreatedGetter)
    }

    property.setter?.let { setter ->
        val recreatedSetter = recreatedProperty.setter
            ?: errorWithCfirSpecificEntries("Recreated setter is missing", cfir = recreatedProperty, psi = recreatedProperty.psi)
        replaceLazyBody(setter, recreatedSetter)
        replaceLazyValueParameters(setter, recreatedSetter)
    }
}

/**
 * 当 [target] 的 initializer 仍需惰性计算时，用 [copy] 的 initializer 替换它。
 */
private fun replaceLazyInitializer(target: CfirVariable, copy: CfirVariable) {
    if (!needCalculatingLazyInitializerForVariable(target)) return
    target.replaceInitializer(copy.initializer)
}

/**
 * 计算变量类声明的惰性 initializer。
 */
private inline fun <reified V : CfirVariable> calculateLazyInitializerForVariable(designation: CfirDesignation) {
    val variable = designation.target as V
    require(needCalculatingLazyInitializerForVariable(variable))

    val recreatedVariable = revive<V>(designation, variable.originalPsi)
    replaceLazyInitializer(variable, recreatedVariable)
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
 * 除显式的 [CfirLazyExpression] 外，仓颉 raw build 在 LAZY_BODIES 下可能留下 `null` initializer；
 * 如果 PSI 仍声明了 initializer，这种 `null` 也必须视为惰性占位。
 */
private fun needCalculatingLazyInitializerForVariable(variable: CfirVariable): Boolean {
    if (variable.initializer is CfirLazyExpression) return true

    /*
     * 对齐 Kotlin 的 LL body-state keeper 语义：未完成的变量 initializer
     * 可能被回写成 lazy placeholder；同时仓颉 raw build 在 LAZY_BODIES 下
     * 也可能直接留下 `null`。两种形态都必须触发重建。
     */
    if (variable.initializer != null) return false
    return (variable.originalPsi as? CjDeclarationWithInitializer)?.hasInitializer() == true
}

/**
 * 计算代码片段的惰性块。
 *
 * 代码片段以文件级片段 PSI 为根重建，随后只替换 [CfirCodeFragment.block]。
 */
private fun calculateLazyBodyForCodeFragment(designation: CfirDesignation) {
    val codeFragment = designation.target as CfirCodeFragment
    require(codeFragment.block is CfirLazyBlock)

    val recreatedCodeFragment = revive<CfirCodeFragment>(designation, codeFragment.psi as? CjCodeFragment)
    codeFragment.replaceBlock(recreatedCodeFragment.block)
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

/**
 * 测试场景使用的全量惰性 body 计算 transformer。
 */
private object CfirAllLazyBodiesCalculatorTransformer : CfirLazyBodiesCalculatorTransformer() {
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
private object CfirTargetLazyBodiesCalculatorTransformer : CfirLazyBodiesCalculatorTransformer()

/**
 * 根据 CFIR 声明种类展开对应惰性 body、initializer 或代码片段块的 transformer 基类。
 */
private sealed class CfirLazyBodiesCalculatorTransformer : CfirTransformer<PersistentList<CfirDeclaration>>() {
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
            calculateLazyBodiesForFunction<CfirNamedFunction>(CfirDesignation(data, namedFunction))
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
            calculateLazyBodiesForFunction<CfirMainFunction>(CfirDesignation(data, mainFunction))
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
            calculateLazyBodiesForFunction<CfirMacroDeclaration>(CfirDesignation(data, macroDeclaration))
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
            calculateLazyBodiesForFunction<CfirFinalizer>(CfirDesignation(data, finalizer))
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
            calculateLazyBodyForConstructor(CfirDesignation(data, constructor))
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
            calculateLazyBodyForConstructor(CfirDesignation(data, errorPrimaryConstructor))
        }
        return errorPrimaryConstructor
    }

    /**
     * 计算属性访问器中的惰性 getter/setter body。
     */
    override fun transformProperty(property: CfirProperty, data: PersistentList<CfirDeclaration>): CfirProperty {
        if (needCalculatingLazyBodyForProperty(property)) {
            calculateLazyBodyForProperty(CfirDesignation(data, property))
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
            calculateLazyInitializerForVariable<CfirFieldVariable>(CfirDesignation(data, fieldVariable))
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
            calculateLazyInitializerForVariable<CfirPatternVariable>(CfirDesignation(data, patternVariable))
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
            calculateLazyBodyForCodeFragment(CfirDesignation(data, codeFragment))
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
