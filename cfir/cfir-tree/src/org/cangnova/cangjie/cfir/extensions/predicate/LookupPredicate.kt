package org.cangnova.cangjie.cfir.extensions.predicate

import org.cangnova.cangjie.cfir.extensions.AnnotationFqn

sealed class LookupPredicate : AbstractPredicate<LookupPredicate> {
    abstract override val annotations: Set<AnnotationFqn>
    final override val metaAnnotations: Set<AnnotationFqn>
        get() = emptySet()

    abstract override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R

    class Or(
        override val a: LookupPredicate,
        override val b: LookupPredicate,
    ) : LookupPredicate(), AbstractPredicate.Or<LookupPredicate> {
        override val annotations: Set<AnnotationFqn> = a.annotations + b.annotations

        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitOr(this, data)
        }
    }

    class And(
        override val a: LookupPredicate,
        override val b: LookupPredicate,
    ) : LookupPredicate(), AbstractPredicate.And<LookupPredicate> {
        override val annotations: Set<AnnotationFqn> = a.annotations + b.annotations

        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitAnd(this, data)
        }
    }

    sealed class Annotated(final override val annotations: Set<AnnotationFqn>) : LookupPredicate(), AbstractPredicate.Annotated<LookupPredicate> {
        init {
            require(annotations.isNotEmpty()) {
                "Annotations should be not empty"
            }
        }

        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitAnnotated(this, data)
        }
    }

    class AnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations), AbstractPredicate.AnnotatedWith<LookupPredicate> {
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitAnnotatedWith(this, data)
        }
    }

    class AncestorAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.AncestorAnnotatedWith<LookupPredicate> {
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitAncestorAnnotatedWith(this, data)
        }
    }

    class ParentAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.ParentAnnotatedWith<LookupPredicate> {
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitParentAnnotatedWith(this, data)
        }
    }

    class HasAnnotatedWith(annotations: Set<AnnotationFqn>) : Annotated(annotations),
        AbstractPredicate.HasAnnotatedWith<LookupPredicate> {
        override fun <R, D> accept(visitor: PredicateVisitor<LookupPredicate, R, D>, data: D): R {
            return visitor.visitHasAnnotatedWith(this, data)
        }
    }

    object BuilderContext : AbstractPredicate.BuilderContext<LookupPredicate>() {
        override infix fun LookupPredicate.or(other: LookupPredicate): LookupPredicate = Or(this, other)
        override infix fun LookupPredicate.and(other: LookupPredicate): LookupPredicate = And(this, other)

        override fun annotated(vararg annotations: AnnotationFqn): LookupPredicate = annotated(annotations.toList())
        override fun ancestorAnnotated(vararg annotations: AnnotationFqn): LookupPredicate = ancestorAnnotated(annotations.toList())
        override fun parentAnnotated(vararg annotations: AnnotationFqn): LookupPredicate = parentAnnotated(annotations.toList())
        override fun hasAnnotated(vararg annotations: AnnotationFqn): LookupPredicate = hasAnnotated(annotations.toList())

        override fun annotatedOrUnder(vararg annotations: AnnotationFqn): LookupPredicate =
            annotated(*annotations) or ancestorAnnotated(*annotations)

        override fun annotated(annotations: Collection<AnnotationFqn>): LookupPredicate = AnnotatedWith(annotations.toSet())
        override fun ancestorAnnotated(annotations: Collection<AnnotationFqn>): LookupPredicate =
            AncestorAnnotatedWith(annotations.toSet())

        override fun parentAnnotated(annotations: Collection<AnnotationFqn>): LookupPredicate =
            ParentAnnotatedWith(annotations.toSet())

        override fun hasAnnotated(annotations: Collection<AnnotationFqn>): LookupPredicate =
            HasAnnotatedWith(annotations.toSet())

        override fun annotatedOrUnder(annotations: Collection<AnnotationFqn>): LookupPredicate =
            annotated(annotations) or ancestorAnnotated(annotations)
    }

    companion object {
        inline fun create(init: BuilderContext.() -> LookupPredicate): LookupPredicate = BuilderContext.init()
    }
}
