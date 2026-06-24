package org.cangnova.cangjie.cfir.extensions.predicate

/**
 * 谓词树 visitor。
 *
 * 泛型 [P] 表示具体谓词族，[R] 表示返回值类型，[D] 表示访问过程携带的数据类型。
 */
abstract class PredicateVisitor<P : AbstractPredicate<P>, R, D> {
    /**
     * 访问任意谓词的默认入口。
     */
    abstract fun visitPredicate(predicate: AbstractPredicate<P>, data: D): R

    /**
     * 访问逻辑与谓词。
     */
    open fun visitAnd(predicate: AbstractPredicate.And<P>, data: D): R {
        return visitPredicate(predicate, data)
    }

    /**
     * 访问逻辑或谓词。
     */
    open fun visitOr(predicate: AbstractPredicate.Or<P>, data: D): R {
        return visitPredicate(predicate, data)
    }

    /**
     * 访问注解相关谓词。
     */
    open fun visitAnnotated(predicate: AbstractPredicate.Annotated<P>, data: D): R {
        return visitPredicate(predicate, data)
    }

    /**
     * 访问声明自身带注解谓词。
     */
    open fun visitAnnotatedWith(predicate: AbstractPredicate.AnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    /**
     * 访问祖先带注解谓词。
     */
    open fun visitAncestorAnnotatedWith(predicate: AbstractPredicate.AncestorAnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    /**
     * 访问直接父声明带注解谓词。
     */
    open fun visitParentAnnotatedWith(predicate: AbstractPredicate.ParentAnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    /**
     * 访问拥有带注解相关声明谓词。
     */
    open fun visitHasAnnotatedWith(predicate: AbstractPredicate.HasAnnotatedWith<P>, data: D): R {
        return visitAnnotated(predicate, data)
    }

    /**
     * 访问元注解谓词。
     */
    open fun visitMetaAnnotatedWith(predicate: AbstractPredicate.MetaAnnotatedWith<P>, data: D): R {
        return visitPredicate(predicate, data)
    }
}

/**
 * 声明谓词 visitor 别名。
 */
@Suppress("unused")
typealias DeclarationPredicateVisitor<R, D> = PredicateVisitor<DeclarationPredicate, R, D>

/**
 * 查找谓词 visitor 别名。
 */
@Suppress("unused")
typealias LookupPredicateVisitor<R, D> = PredicateVisitor<LookupPredicate, R, D>
