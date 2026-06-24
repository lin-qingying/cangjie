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
    /**
     * 当前谓词直接依赖的注解 FQN 集合。
     */
    val annotations: Set<AnnotationFqn>

    /**
     * 当前谓词依赖的元注解 FQN 集合。
     */
    val metaAnnotations: Set<AnnotationFqn>

    /**
     * 接受谓词 visitor。
     */
    fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R

    /**
     * 逻辑或谓词。
     */
    sealed interface Or<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        /**
         * 左侧谓词。
         */
        val a: P

        /**
         * 右侧谓词。
         */
        val b: P

        /**
         * 分派到 [PredicateVisitor.visitOr]。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitOr(this, data)
        }
    }

    /**
     * 逻辑与谓词。
     */
    sealed interface And<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        /**
         * 左侧谓词。
         */
        val a: P

        /**
         * 右侧谓词。
         */
        val b: P

        /**
         * 分派到 [PredicateVisitor.visitAnd]。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnd(this, data)
        }
    }

    /**
     * 注解相关谓词的公共基类。
     */
    sealed interface Annotated<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        /**
         * 分派到 [PredicateVisitor.visitAnnotated]。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnnotated(this, data)
        }
    }

    /**
     * 声明自身带有指定注解的谓词。
     */
    sealed interface AnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        /**
         * 分派到 [PredicateVisitor.visitAnnotatedWith]。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAnnotatedWith(this, data)
        }
    }

    /**
     * 任一祖先声明带有指定注解的谓词。
     */
    sealed interface AncestorAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        /**
         * 分派到 [PredicateVisitor.visitAncestorAnnotatedWith]。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitAncestorAnnotatedWith(this, data)
        }
    }

    /**
     * 直接父声明带有指定注解的谓词。
     */
    sealed interface ParentAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        /**
         * 分派到 [PredicateVisitor.visitParentAnnotatedWith]。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitParentAnnotatedWith(this, data)
        }
    }

    /**
     * 当前声明拥有带指定注解的相关声明的谓词。
     */
    sealed interface HasAnnotatedWith<P : AbstractPredicate<P>> : Annotated<P> {
        /**
         * 分派到 [PredicateVisitor.visitHasAnnotatedWith]。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitHasAnnotatedWith(this, data)
        }
    }

    /**
     * 声明注解本身带有指定元注解的谓词。
     */
    sealed interface MetaAnnotatedWith<P : AbstractPredicate<P>> : AbstractPredicate<P> {
        /**
         * 匹配时是否把声明自身注解也纳入候选。
         */
        val includeItself: Boolean

        /**
         * 分派到 [PredicateVisitor.visitMetaAnnotatedWith]。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<P, R, D>, data: D): R {
            return visitor.visitMetaAnnotatedWith(this, data)
        }
    }

    /**
     * 谓词 DSL 构建上下文。
     */
    abstract class BuilderContext<P : AbstractPredicate<P>> {
        /**
         * 构造逻辑或谓词。
         */
        abstract infix fun P.or(other: P): P

        /**
         * 构造逻辑与谓词。
         */
        abstract infix fun P.and(other: P): P

        /**
         * 构造“声明自身带注解”谓词。
         */
        abstract fun annotated(vararg annotations: AnnotationFqn): P

        /**
         * 构造“祖先带注解”谓词。
         */
        abstract fun ancestorAnnotated(vararg annotations: AnnotationFqn): P

        /**
         * 构造“父声明带注解”谓词。
         */
        abstract fun parentAnnotated(vararg annotations: AnnotationFqn): P

        /**
         * 构造“拥有带注解相关声明”谓词。
         */
        abstract fun hasAnnotated(vararg annotations: AnnotationFqn): P

        /**
         * 构造“自身或祖先带注解”谓词。
         */
        abstract fun annotatedOrUnder(vararg annotations: AnnotationFqn): P

        /**
         * 使用集合参数构造“声明自身带注解”谓词。
         */
        abstract fun annotated(annotations: Collection<AnnotationFqn>): P

        /**
         * 使用集合参数构造“祖先带注解”谓词。
         */
        abstract fun ancestorAnnotated(annotations: Collection<AnnotationFqn>): P

        /**
         * 使用集合参数构造“父声明带注解”谓词。
         */
        abstract fun parentAnnotated(annotations: Collection<AnnotationFqn>): P

        /**
         * 使用集合参数构造“拥有带注解相关声明”谓词。
         */
        abstract fun hasAnnotated(annotations: Collection<AnnotationFqn>): P

        /**
         * 使用集合参数构造“自身或祖先带注解”谓词。
         */
        abstract fun annotatedOrUnder(annotations: Collection<AnnotationFqn>): P
    }
}
