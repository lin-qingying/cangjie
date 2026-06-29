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
    /**
     * 已经转换完成的公开注解对象列表。
     */
    private val backingAnnotations: List<CaAnnotation>,
    /**
     * 约束注解列表生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : AbstractList<CaAnnotation>(), CaAnnotationList {
    /**
     * 注解列表中的注解数量。
     */
    override val size: Int
        get() = withValidityAssertion { backingAnnotations.size }

    /**
     * 返回按源码/CFIR 顺序遍历注解的迭代器。
     */
    override fun iterator(): Iterator<CaAnnotation> = withValidityAssertion {
        backingAnnotations.iterator()
    }

    /**
     * 按列表下标返回对应注解。
     */
    override fun get(index: Int): CaAnnotation = withValidityAssertion {
        backingAnnotations[index]
    }

    /**
     * 判断列表中是否存在指定 classId 的注解。
     */
    override fun contains(classId: ClassId): Boolean = withValidityAssertion {
        backingAnnotations.any { annotation -> annotation.classId == classId }
    }

    /**
     * 返回所有匹配指定 classId 的注解。
     */
    override fun get(classId: ClassId): List<CaAnnotation> = withValidityAssertion {
        backingAnnotations.filter { annotation -> annotation.classId == classId }
    }

    /**
     * 返回列表中所有可解析出的注解 classId。
     */
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
