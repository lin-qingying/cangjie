package org.cangnova.cangjie.analysis.api.impl.base.annotations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationValue
import org.cangnova.cangjie.analysis.api.annotations.CaNamedAnnotationValue
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.name.Name

/**
 * 命名注解参数的基础实现。
 *
 * backend 不应再把注解参数退化成字符串列表，而应把参数名和值分开建模。
 */
public class CaBaseNamedAnnotationValue(
    name: Name,
    expression: CaAnnotationValue,
) : CaNamedAnnotationValue {
    private val backingName: Name = name
    private val backingExpression: CaAnnotationValue = expression

    override val token: CaLifetimeToken
        get() = backingExpression.token

    override val name: Name
        get() = withValidityAssertion { backingName }

    override val expression: CaAnnotationValue
        get() = withValidityAssertion { backingExpression }
}
