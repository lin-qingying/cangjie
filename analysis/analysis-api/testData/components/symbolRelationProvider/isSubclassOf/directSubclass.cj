// FILE: directSubclass.cj
// TARGET_CLASS: Child
// SUPER_CLASS_NAME: Parent
// EXPECTED_IS_SUBCLASS: true
// EXPECTED_IS_DIRECT_SUBCLASS: true
package sample.symbol.relation.subclass

open class Parent {
}

class Child <: Parent {
}
