package org.cangnova.cangjie.cfir.extensions.predicate

abstract class PredicateVisitor<P : AbstractPredicate<P>, R, D> {
    abstract fun visitPredicate(predicate: AbstractPredicate<P>, data: D): R

    open fun visitAnd(predicate: AbstractPredicate.And<P>, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitOr(predicate: AbstractPredicate.Or<P>, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitAnnotated(predicate: AbstractPredicate.Annotated<P>, data: D): R {
        return visitPredicate(predicate, data)
    }

    open fun visitAnnotatedWith(predicate: AbstractPredicate.AnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    open fun visitAncestorAnnotatedWith(predicate: AbstractPredicate.AncestorAnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    open fun visitParentAnnotatedWith(predicate: AbstractPredicate.ParentAnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    open fun visitHasAnnotatedWith(predicate: AbstractPredicate.HasAnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    open fun visitMetaAnnotatedWith(predicate: AbstractPredicate.MetaAnnotatedWith<P>, data: D): R {
        return visitPredicate(predicate, data)
    }
}

@Suppress("unused")
typealias DeclarationPredicateVisitor<R, D> = PredicateVisitor<DeclarationPredicate, R, D>

@Suppress("unused")
typealias LookupPredicateVisitor<R, D> = PredicateVisitor<LookupPredicate, R, D>
