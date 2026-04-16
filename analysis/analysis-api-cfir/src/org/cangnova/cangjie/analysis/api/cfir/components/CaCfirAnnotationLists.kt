package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseEmptyAnnotationList
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.name.ClassId

/**
 * CFIR 侧公共注解列表实现。
 *
 * Kotlin FIR 后端会把 declaration/type 的注解统一包装成 `KaAnnotationList`，
 * 而不是在各个 symbol/type 上直接暴露裸 `List<Annotation>`。
 * 这里沿用同样的设计，把已经构造好的公开注解对象收束为稳定的 `CaAnnotationList`。
 */
internal class CaCfirAnnotationList(
    private val backingAnnotations: List<CaAnnotation>,
    override val token: CaLifetimeToken,
) : AbstractList<CaAnnotation>(), CaAnnotationList {
    override val size: Int
        get() = withValidityAssertion { backingAnnotations.size }

    override fun iterator(): Iterator<CaAnnotation> = withValidityAssertion {
        backingAnnotations.iterator()
    }

    override fun get(index: Int): CaAnnotation = withValidityAssertion {
        backingAnnotations[index]
    }

    override fun contains(classId: ClassId): Boolean = withValidityAssertion {
        backingAnnotations.any { annotation -> annotation.classId == classId }
    }

    override fun get(classId: ClassId): List<CaAnnotation> = withValidityAssertion {
        backingAnnotations.filter { annotation -> annotation.classId == classId }
    }

    override val classIds: Collection<ClassId>
        get() = withValidityAssertion {
            backingAnnotations.mapNotNull { annotation -> annotation.classId }
        }
}

/**
 * 将已经构造完成的公开注解对象提升为稳定的 `CaAnnotationList`。
 */
internal fun List<CaAnnotation>.asCaAnnotationList(token: CaLifetimeToken): CaAnnotationList {
    return if (isEmpty()) {
        CaBaseEmptyAnnotationList(token)
    } else {
        CaCfirAnnotationList(this, token)
    }
}
