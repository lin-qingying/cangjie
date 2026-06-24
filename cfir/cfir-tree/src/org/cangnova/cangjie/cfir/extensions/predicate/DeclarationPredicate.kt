package org.cangnova.cangjie.cfir.extensions.predicate

import org.cangnova.cangjie.cfir.extensions.AnnotationFqn

/**
 * 针对声明树匹配的插件谓词。
 */
sealed class DeclarationPredicate : AbstractPredicate<DeclarationPredicate> {
    /**
     * 当前谓词直接依赖的注解集合。
     */
    abstract override val annotations: Set<AnnotationFqn>

    /**
     * 当前谓词依赖的元注解集合。
     */
    abstract override val metaAnnotations: Set<AnnotationFqn>

    /**
     * 接受声明谓词 visitor。
     */
    abstract override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R

    /**
     * 声明谓词逻辑或节点。
     *
     * @property a 左侧谓词。
     * @property b 右侧谓词。
     */
    class Or(
        override val a: DeclarationPredicate,
        override val b: DeclarationPredicate,
    ) : DeclarationPredicate(), AbstractPredicate.Or<DeclarationPredicate> {
        /**
         * 两侧谓词依赖注解的并集。
         */
        override val annotations: Set<AnnotationFqn> = a.annotations + b.annotations

        /**
         * 两侧谓词依赖元注解的并集。
         */
        override val metaAnnotations: Set<AnnotationFqn> = a.metaAnnotations + b.metaAnnotations

        /**
         * 分派到 visitor 的 or 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitOr(this, data)
        }
    }

    /**
     * 声明谓词逻辑与节点。
     *
     * @property a 左侧谓词。
     * @property b 右侧谓词。
     */
    class And(
        override val a: DeclarationPredicate,
        override val b: DeclarationPredicate,
    ) : DeclarationPredicate(), AbstractPredicate.And<DeclarationPredicate> {
        /**
         * 两侧谓词依赖注解的并集。
         */
        override val annotations: Set<AnnotationFqn> = a.annotations + b.annotations

        /**
         * 两侧谓词依赖元注解的并集。
         */
        override val metaAnnotations: Set<AnnotationFqn> = a.metaAnnotations + b.metaAnnotations

        /**
         * 分派到 visitor 的 and 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitAnd(this, data)
        }
    }

    /**
     * 声明注解谓词公共基类。
     *
     * @property annotations 当前谓词匹配的注解集合。
     */
    sealed class Annotated(final override val annotations: Set<AnnotationFqn>) : DeclarationPredicate(),
        AbstractPredicate.Annotated<DeclarationPredicate> {
        init {
            require(annotations.isNotEmpty()) {
                "Annotations should be not empty"
            }
        }

        /**
         * 普通注解谓词不依赖元注解。
         */
        final override val metaAnnotations: Set<AnnotationFqn>
            get() = emptySet()

        /**
         * 分派到 visitor 的 annotated 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitAnnotated(this, data)
        }
    }

    /**
     * 声明自身带有指定注解的谓词。
     */
    class AnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations), AbstractPredicate.AnnotatedWith<DeclarationPredicate> {
        /**
         * 分派到 visitor 的 annotated-with 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitAnnotatedWith(this, data)
        }
    }

    /**
     * 任一祖先声明带有指定注解的谓词。
     */
    class AncestorAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.AncestorAnnotatedWith<DeclarationPredicate> {
        /**
         * 分派到 visitor 的 ancestor-annotated-with 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitAncestorAnnotatedWith(this, data)
        }
    }

    /**
     * 直接父声明带有指定注解的谓词。
     */
    class ParentAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.ParentAnnotatedWith<DeclarationPredicate> {
        /**
         * 分派到 visitor 的 parent-annotated-with 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitParentAnnotatedWith(this, data)
        }
    }

    /**
     * 当前声明拥有带指定注解的相关声明的谓词。
     */
    class HasAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.HasAnnotatedWith<DeclarationPredicate> {
        /**
         * 分派到 visitor 的 has-annotated-with 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitHasAnnotatedWith(this, data)
        }
    }

    /**
     * 声明注解带有指定元注解的谓词。
     *
     * @property metaAnnotations 匹配的元注解集合。
     * @property includeItself 是否把声明自身注解也纳入匹配。
     */
    class MetaAnnotatedWith(
        override val metaAnnotations: Set<AnnotationFqn>,
        override val includeItself: Boolean,
    ) : DeclarationPredicate(), AbstractPredicate.MetaAnnotatedWith<DeclarationPredicate> {
        init {
            require(metaAnnotations.isNotEmpty()) {
                "Annotations should be not empty"
            }
        }

        /**
         * 元注解谓词不直接依赖普通注解集合。
         */
        override val annotations: Set<AnnotationFqn>
            get() = emptySet()

        /**
         * 分派到 visitor 的 meta-annotated-with 入口。
         */
        override fun <R, D> accept(visitor: PredicateVisitor<DeclarationPredicate, R, D>, data: D): R {
            return visitor.visitMetaAnnotatedWith(this, data)
        }
    }

    /**
     * 声明谓词 DSL builder。
     */
    object BuilderContext : AbstractPredicate.BuilderContext<DeclarationPredicate>() {
        /**
         * 构造逻辑或谓词。
         */
        override infix fun DeclarationPredicate.or(other: DeclarationPredicate): DeclarationPredicate = Or(this, other)

        /**
         * 构造逻辑与谓词。
         */
        override infix fun DeclarationPredicate.and(other: DeclarationPredicate): DeclarationPredicate = And(this, other)

        /**
         * 构造声明自身带注解谓词。
         */
        override fun annotated(vararg annotations: AnnotationFqn): DeclarationPredicate = annotated(annotations.toList())

        /**
         * 构造祖先带注解谓词。
         */
        override fun ancestorAnnotated(vararg annotations: AnnotationFqn): DeclarationPredicate = ancestorAnnotated(annotations.toList())

        /**
         * 构造父声明带注解谓词。
         */
        override fun parentAnnotated(vararg annotations: AnnotationFqn): DeclarationPredicate = parentAnnotated(annotations.toList())

        /**
         * 构造拥有带注解相关声明谓词。
         */
        override fun hasAnnotated(vararg annotations: AnnotationFqn): DeclarationPredicate = hasAnnotated(annotations.toList())

        /**
         * 构造自身或祖先带注解谓词。
         */
        override fun annotatedOrUnder(vararg annotations: AnnotationFqn): DeclarationPredicate =
            annotated(*annotations) or ancestorAnnotated(*annotations)

        /**
         * 构造元注解谓词。
         */
        fun metaAnnotated(vararg metaAnnotations: AnnotationFqn, includeItself: Boolean): DeclarationPredicate =
            MetaAnnotatedWith(metaAnnotations.toSet(), includeItself)

        /**
         * 使用集合参数构造声明自身带注解谓词。
         */
        override fun annotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate = AnnotatedWith(annotations.toSet())

        /**
         * 使用集合参数构造祖先带注解谓词。
         */
        override fun ancestorAnnotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate =
            AncestorAnnotatedWith(annotations.toSet())

        /**
         * 使用集合参数构造父声明带注解谓词。
         */
        override fun parentAnnotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate =
            ParentAnnotatedWith(annotations.toSet())

        /**
         * 使用集合参数构造拥有带注解相关声明谓词。
         */
        override fun hasAnnotated(annotations: Collection<AnnotationFqn>): DeclarationPredicate =
            HasAnnotatedWith(annotations.toSet())

        /**
         * 使用集合参数构造自身或祖先带注解谓词。
         */
        override fun annotatedOrUnder(annotations: Collection<AnnotationFqn>): DeclarationPredicate =
            annotated(annotations) or ancestorAnnotated(annotations)

        /**
         * 使用集合参数构造元注解谓词。
         */
        fun metaAnnotated(metaAnnotations: Collection<AnnotationFqn>, includeItself: Boolean): DeclarationPredicate =
            MetaAnnotatedWith(metaAnnotations.toSet(), includeItself)
    }

    /**
     * 声明谓词工厂入口。
     */
    companion object {
        /**
         * 使用 DSL 创建声明谓词。
         */
        inline fun create(init: BuilderContext.() -> DeclarationPredicate): DeclarationPredicate = BuilderContext.init()
    }
}
