package org.cangnova.cangjie.cfir.extensions.predicate

import org.cangnova.cangjie.cfir.extensions.AnnotationFqn

/**
 * 用于符号查找索引的插件谓词。
 *
 * lookup 谓词只依赖普通注解集合，不携带元注解集合。
 */
sealed class LookupPredicate : AbstractPredicate<LookupPredicate> {
    /**
     * 当前 lookup 谓词依赖的注解集合。
     */
    abstract override val annotations: Set<AnnotationFqn>

    /**
     * lookup 谓词不直接保存元注解。
     */
    final override val metaAnnotations: Set<AnnotationFqn>
        get() = emptySet()

    /**
     * 接受 lookup 谓词 visitor。
     */
    abstract override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R

    /**
     * lookup 谓词逻辑或节点。
     */
    class Or(
        override val a: LookupPredicate,
        override val b: LookupPredicate,
    ) : LookupPredicate(), AbstractPredicate.Or<LookupPredicate> {
        /**
         * 两侧谓词依赖注解的并集。
         */
        override val annotations: Set<AnnotationFqn> = a.annotations + b.annotations

        /**
         * 分派到 visitor 的 or 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitOr(this, data)
        }
    }

    /**
     * lookup 谓词逻辑与节点。
     */
    class And(
        override val a: LookupPredicate,
        override val b: LookupPredicate,
    ) : LookupPredicate(), AbstractPredicate.And<LookupPredicate> {
        /**
         * 两侧谓词依赖注解的并集。
         */
        override val annotations: Set<AnnotationFqn> = a.annotations + b.annotations

        /**
         * 分派到 visitor 的 and 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitAnd(this, data)
        }
    }

    /**
     * lookup 注解谓词公共基类。
     */
    sealed class Annotated(final override val annotations: Set<AnnotationFqn>) : LookupPredicate(), AbstractPredicate.Annotated<LookupPredicate> {
        init {
            require(annotations.isNotEmpty()) {
                "Annotations should be not empty"
            }
        }

        /**
         * 分派到 visitor 的 annotated 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitAnnotated(this, data)
        }
    }

    /**
     * 声明自身带指定注解的 lookup 谓词。
     */
    class AnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations), AbstractPredicate.AnnotatedWith<LookupPredicate> {
        /**
         * 分派到 visitor 的 annotated-with 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitAnnotatedWith(this, data)
        }
    }

    /**
     * 祖先带指定注解的 lookup 谓词。
     */
    class AncestorAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.AncestorAnnotatedWith<LookupPredicate> {
        /**
         * 分派到 visitor 的 ancestor-annotated-with 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitAncestorAnnotatedWith(this, data)
        }
    }

    /**
     * 直接父声明带指定注解的 lookup 谓词。
     */
    class ParentAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.ParentAnnotatedWith<LookupPredicate> {
        /**
         * 分派到 visitor 的 parent-annotated-with 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitParentAnnotatedWith(this, data)
        }
    }

    /**
     * 拥有带指定注解相关声明的 lookup 谓词。
     */
    class HasAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.HasAnnotatedWith<LookupPredicate> {
        /**
         * 分派到 visitor 的 has-annotated-with 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitHasAnnotatedWith(this, data)
        }
    }

    /**
     * lookup 谓词 DSL builder。
     */
    object BuilderContext : AbstractPredicate.BuilderContext<LookupPredicate>() {
        /**
         * 构造逻辑或谓词。
         */
        override infix fun LookupPredicate.or(other: LookupPredicate): LookupPredicate = Or(this, other)

        /**
         * 构造逻辑与谓词。
         */
        override infix fun LookupPredicate.and(other: LookupPredicate): LookupPredicate = And(this, other)

        /**
         * 构造声明自身带注解谓词。
         */
        override fun annotated(vararg annotations: AnnotationFqn): LookupPredicate = annotated(annotations.toList())

        /**
         * 构造祖先带注解谓词。
         */
        override fun ancestorAnnotated(vararg annotations: AnnotationFqn): LookupPredicate = ancestorAnnotated(annotations.toList())

        /**
         * 构造父声明带注解谓词。
         */
        override fun parentAnnotated(vararg annotations: AnnotationFqn): LookupPredicate = parentAnnotated(annotations.toList())

        /**
         * 构造拥有带注解相关声明谓词。
         */
        override fun hasAnnotated(vararg annotations: AnnotationFqn): LookupPredicate = hasAnnotated(annotations.toList())

        /**
         * 构造自身或祖先带注解谓词。
         */
        override fun annotatedOrUnder(vararg annotations: AnnotationFqn): LookupPredicate =
            annotated(*annotations) or ancestorAnnotated(*annotations)

        /**
         * 使用集合参数构造声明自身带注解谓词。
         */
        override fun annotated(annotations: Collection<AnnotationFqn>): LookupPredicate = AnnotatedWith(annotations.toSet())

        /**
         * 使用集合参数构造祖先带注解谓词。
         */
        override fun ancestorAnnotated(annotations: Collection<AnnotationFqn>): LookupPredicate =
            AncestorAnnotatedWith(annotations.toSet())

        /**
         * 使用集合参数构造父声明带注解谓词。
         */
        override fun parentAnnotated(annotations: Collection<AnnotationFqn>): LookupPredicate =
            ParentAnnotatedWith(annotations.toSet())

        /**
         * 使用集合参数构造拥有带注解相关声明谓词。
         */
        override fun hasAnnotated(annotations: Collection<AnnotationFqn>): LookupPredicate =
            HasAnnotatedWith(annotations.toSet())

        /**
         * 使用集合参数构造自身或祖先带注解谓词。
         */
        override fun annotatedOrUnder(annotations: Collection<AnnotationFqn>): LookupPredicate =
            annotated(annotations) or ancestorAnnotated(annotations)
    }

    /**
     * lookup 谓词工厂入口。
     */
    companion object {
        /**
         * 使用 DSL 创建 lookup 谓词。
         */
        inline fun create(init: BuilderContext.() -> LookupPredicate): LookupPredicate = BuilderContext.init()
    }
}
