package org.cangnova.cangjie.analysis.api.impl.base.annotations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.name.ClassId
import java.util.Collections

/**
 * 空注解列表的基础实现。
 *
 * Kotlin Analysis API 在 impl-base 中同样提供了独立的 empty annotation list，
 * 这样各后端在“确实没有注解”的场景下可以复用统一实现，而不是各自返回裸 `emptyList()`，
 * 从而保持 `CaAnnotationList` 的稳定语义边界。
 */
public class CaBaseEmptyAnnotationList(
    override val token: CaLifetimeToken,
) : AbstractList<CaAnnotation>(), CaAnnotationList {
    override val size: Int
        get() = withValidityAssertion { 0 }

    override fun iterator(): Iterator<CaAnnotation> = withValidityAssertion {
        Collections.emptyIterator()
    }

    override fun get(index: Int): CaAnnotation = withValidityAssertion {
        throw IndexOutOfBoundsException("Index $index out of bounds")
    }

    override fun contains(classId: ClassId): Boolean = withValidityAssertion {
        false
    }

    override fun get(classId: ClassId): List<CaAnnotation> = withValidityAssertion {
        emptyList()
    }

    override val classIds: Set<ClassId>
        get() = withValidityAssertion { emptySet() }
}
