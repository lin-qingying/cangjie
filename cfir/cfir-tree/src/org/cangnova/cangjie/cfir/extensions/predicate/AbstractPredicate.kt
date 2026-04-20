package org.cangnova.cangjie.cfir.extensions.predicate

import org.cangnova.cangjie.cfir.extensions.AnnotationFqn
import org.cangnova.cangjie.cfir.extensions.CfirPredicateBasedProvider

/**
 * predicate 体系对位 Kotlin FIR。
 *
 * 仓颉当前不支持 local / nested class，因此 low-level 的匹配和全局查找会天然收紧到
 * 顶层与成员声明；这里仍保持与 Kotlin 相同的主抽象层次。
 */
sealed interface AbstractPredicate<P : AbstractPredicate<P>> {
    val annotations: Set<AnnotationFqn>
    val metaAnnotations: Set<AnnotationFqn>

    fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R

    sealed interface Or<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        val a: P
        val b: P

        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitOr(this, data)
        }
    }

    sealed interface And<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        val a: P
        val b: P

        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnd(this, data)
        }
    }

    sealed interface Annotated<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnnotated(this, data)
        }
    }

    sealed interface AnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnnotatedWith(this, data)
        }
    }

    sealed interface AncestorAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAncestorAnnotatedWith(this, data)
        }
    }

    sealed interface ParentAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitParentAnnotatedWith(this, data)
        }
    }

    sealed interface HasAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitHasAnnotatedWith(this, data)
        }
    }

    sealed interface MetaAnnotatedWith<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        val includeItself: Boolean

        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitMetaAnnotatedWith(this, data)
        }
    }

    abstract class BuilderContext<P : AbstractPredicate<P>> {
        abstract infix fun P.or(other: P): P
        abstract infix fun P.and(other: P): P

        abstract fun annotated(vararg annotations: AnnotationFqn): P
        abstract fun ancestorAnnotated(vararg annotations: AnnotationFqn): P
        abstract fun parentAnnotated(vararg annotations: AnnotationFqn): P
        abstract fun hasAnnotated(vararg annotations: AnnotationFqn): P

        abstract fun annotatedOrUnder(vararg annotations: AnnotationFqn): P

        abstract fun annotated(annotations: Collection<AnnotationFqn>): P
        abstract fun ancestorAnnotated(annotations: Collection<AnnotationFqn>): P
        abstract fun parentAnnotated(annotations: Collection<AnnotationFqn>): P
        abstract fun hasAnnotated(annotations: Collection<AnnotationFqn>): P

        abstract fun annotatedOrUnder(annotations: Collection<AnnotationFqn>): P
    }
}
