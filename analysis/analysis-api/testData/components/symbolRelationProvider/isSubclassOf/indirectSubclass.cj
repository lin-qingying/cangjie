// MODULE: main
// MAIN_MODULE
// MAIN_FILE_NAME: indirectSubclass.cj
// FILE: hierarchy.cj
package sample.symbol.relation.subclass

open class Root {
}

open class Parent <: Root {
}

// FILE: indirectSubclass.cj
// TARGET_CLASS: Child
// SUPER_CLASS_NAME: Root
// EXPECTED_IS_SUBCLASS: true
// EXPECTED_IS_DIRECT_SUBCLASS: false
package sample.symbol.relation.subclass

class Child <: Parent {
}
