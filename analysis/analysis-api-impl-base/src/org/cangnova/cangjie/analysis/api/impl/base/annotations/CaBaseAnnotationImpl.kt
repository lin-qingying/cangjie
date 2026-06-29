package org.cangnova.cangjie.analysis.api.impl.base.annotations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallElement

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
) : CaAnnotation {
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
}
