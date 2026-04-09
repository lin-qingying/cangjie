// FILE: localClasses.cj
// TARGET_CLASS: LocalChild
// SUPER_CLASS_NAME: LocalRoot
// EXPECTED_IS_SUBCLASS: true
// EXPECTED_IS_DIRECT_SUBCLASS: true
package sample.symbol.relation.subclass

func buildTree(): Int64 {
    open class LocalRoot {
    }

    class LocalChild <: LocalRoot {
    }

    return 0
}
