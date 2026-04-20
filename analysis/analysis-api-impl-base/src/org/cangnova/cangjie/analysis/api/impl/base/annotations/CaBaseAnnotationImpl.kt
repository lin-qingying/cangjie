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
    override val token: CaLifetimeToken,
) : CaAnnotation {
    private val backingClassId: ClassId? = classId
    private val backingShortName: Name? = shortName
    private val backingPsi: CjCallElement? = psi
    private val backingArguments: List<CaNamedAnnotationValue> by lazyArguments
    private val backingConstructorSymbol: CaConstructorSymbol? = constructorSymbol

    override val classId: ClassId?
        get() = withValidityAssertion { backingClassId }

    override val shortName: Name?
        get() = withValidityAssertion { backingShortName }

    override val psi: CjCallElement?
        get() = withValidityAssertion { backingPsi }

    override val arguments: List<CaNamedAnnotationValue>
        get() = withValidityAssertion { backingArguments }

    override val constructorSymbol: CaConstructorSymbol?
        get() = withValidityAssertion { backingConstructorSymbol }
}
