package org.cangnova.cangjie.analysis.api.impl.base.annotations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationResolutionStatus
import org.cangnova.cangjie.analysis.api.annotations.CaNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallElement
import java.util.Objects

/**
 * Analysis API 注解应用的基础实现。
 *
 * 这个实现对齐当前公开协议：
 * - 注解身份由 `classId/shortName` 表达
 * - 源码形态由 `psi` 表达
 * - 参数列表与构造器符号分别独立暴露
 *
 * 参数采用惰性求值，避免在只读取限定名时提前构造参数值对象。
 */
public class CaBaseAnnotationImpl(
    classId: ClassId?,
    shortName: Name?,
    psi: CjCallElement?,
    lazyArguments: Lazy<List<CaNamedAnnotationValue>>,
    constructorSymbol: CaConstructorSymbol?,
    /**
     * 该注解对象绑定的 lifetime token。
     */
    override val token: CaLifetimeToken,
    builtInKind: org.cangnova.cangjie.annotations.BuiltInAnnotationKind? = null,
    isCompileTimeVisible: Boolean? = null,
    isForcedCustom: Boolean? = null,
    target: org.cangnova.cangjie.annotations.CangjieAnnotationTarget? = null,
    runtimeVisible: Boolean? = null,
    resolutionStatus: CaAnnotationResolutionStatus = CaAnnotationResolutionStatus.UNKNOWN,
    evaluatedInstance: CaAnnotationValue? = null,
) : CaAnnotation {
    private val backingBuiltInKind = builtInKind
    private val backingCompileTimeVisible = isCompileTimeVisible
    private val backingForcedCustom = isForcedCustom
    private val backingTarget = target
    private val backingRuntimeVisible = runtimeVisible
    private val backingResolutionStatus = resolutionStatus
    private val backingEvaluatedInstance = evaluatedInstance
    override val builtInKind: org.cangnova.cangjie.annotations.BuiltInAnnotationKind?
        get() = withValidityAssertion { backingBuiltInKind }
    override val isCompileTimeVisible: Boolean?
        get() = withValidityAssertion { backingCompileTimeVisible }
    override val isForcedCustom: Boolean?
        get() = withValidityAssertion { backingForcedCustom }
    override val target: org.cangnova.cangjie.annotations.CangjieAnnotationTarget?
        get() = withValidityAssertion { backingTarget }
    override val runtimeVisible: Boolean?
        get() = withValidityAssertion { backingRuntimeVisible }
    override val resolutionStatus: CaAnnotationResolutionStatus
        get() = withValidityAssertion { backingResolutionStatus }
    override val evaluatedInstance: CaAnnotationValue?
        get() = withValidityAssertion { backingEvaluatedInstance }
    /**
     * 注解类型的稳定 classId。
     */
    private val backingClassId: ClassId? = classId

    /**
     * 注解类型的短名。
     */
    private val backingShortName: Name? = shortName

    /**
     * 源码中的注解调用 PSI。
     */
    private val backingPsi: CjCallElement? = psi

    /**
     * 惰性解析后的注解参数列表。
     */
    private val backingArguments: List<CaNamedAnnotationValue> by lazyArguments

    /**
     * 注解构造器符号。
     */
    private val backingConstructorSymbol: CaConstructorSymbol? = constructorSymbol

    /**
     * 返回注解类型的 classId。
     */
    override val classId: ClassId?
        get() = withValidityAssertion { backingClassId }

    /**
     * 返回注解类型的短名。
     */
    override val shortName: Name?
        get() = withValidityAssertion { backingShortName }

    /**
     * 返回注解调用 PSI。
     */
    override val psi: CjCallElement?
        get() = withValidityAssertion { backingPsi }

    /**
     * 返回注解参数列表。
     */
    override val arguments: List<CaNamedAnnotationValue>
        get() = withValidityAssertion { backingArguments }

    /**
     * 返回注解构造器符号。
     */
    override val constructorSymbol: CaConstructorSymbol?
        get() = withValidityAssertion { backingConstructorSymbol }

    /**
     * Analysis API 注解对象按语义载荷比较，而不是按每次投影产生的 JVM 对象地址比较。
     * lifetime token 不参与比较；它只是访问有效性的边界。
     */
    override fun equals(other: Any?): Boolean {
        return this === other || other is CaBaseAnnotationImpl &&
            backingClassId == other.backingClassId &&
            backingShortName == other.backingShortName &&
            backingPsi == other.backingPsi &&
            backingBuiltInKind == other.backingBuiltInKind &&
            backingCompileTimeVisible == other.backingCompileTimeVisible &&
            backingForcedCustom == other.backingForcedCustom &&
            backingTarget == other.backingTarget &&
            backingRuntimeVisible == other.backingRuntimeVisible &&
            backingResolutionStatus == other.backingResolutionStatus &&
            backingConstructorSymbol == other.backingConstructorSymbol &&
            backingArguments == other.backingArguments &&
            backingEvaluatedInstance == other.backingEvaluatedInstance
    }

    override fun hashCode(): Int = Objects.hash(
        backingClassId,
        backingShortName,
        backingPsi,
        backingBuiltInKind,
        backingCompileTimeVisible,
        backingForcedCustom,
        backingTarget,
        backingRuntimeVisible,
        backingResolutionStatus,
        backingConstructorSymbol,
        backingArguments,
        backingEvaluatedInstance,
    )
}
