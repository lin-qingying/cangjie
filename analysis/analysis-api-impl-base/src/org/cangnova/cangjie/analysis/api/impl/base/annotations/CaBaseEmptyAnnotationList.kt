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
    /**
     * 空注解列表绑定的 lifetime token。
     */
    override val token: CaLifetimeToken,
) : AbstractList<CaAnnotation>(), CaAnnotationList {
    /**
     * 空注解列表的元素数量恒为 0。
     */
    override val size: Int
        get() = withValidityAssertion { 0 }

    /**
     * 返回空迭代器。
     */
    override fun iterator(): Iterator<CaAnnotation> = withValidityAssertion {
        Collections.emptyIterator()
    }

    /**
     * 空列表不允许按索引读取注解。
     */
    override fun get(index: Int): CaAnnotation = withValidityAssertion {
        throw IndexOutOfBoundsException("Index $index out of bounds")
    }

    /**
     * 空注解列表不包含任何 classId。
     */
    override fun contains(classId: ClassId): Boolean = withValidityAssertion {
        false
    }

    /**
     * 空注解列表对任意 classId 都返回空结果。
     */
    override fun get(classId: ClassId): List<CaAnnotation> = withValidityAssertion {
        emptyList()
    }

    /**
     * 空注解列表没有任何注解 classId。
     */
    override val classIds: Set<ClassId>
        get() = withValidityAssertion { emptySet() }
}
