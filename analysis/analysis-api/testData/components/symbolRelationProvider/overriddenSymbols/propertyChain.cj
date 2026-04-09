// FILE: propertyChain.cj
// TARGET_CLASS: Child
// TARGET_NAME: state
// OVERRIDE_TARGET_KIND: MEMBER_PROPERTY
// EXPECTED_ALL_OVERRIDDEN: sample.symbol.relation.overrides.Parent.state:Int64
// EXPECTED_ALL_OVERRIDDEN: sample.symbol.relation.overrides.Root.state:Int64
// EXPECTED_DIRECT_OVERRIDDEN: sample.symbol.relation.overrides.Parent.state:Int64
package sample.symbol.relation.overrides

open class Root {
    prop state: Int64 {
        get() {
            1
        }
    }
}

open class Parent <: Root {
    prop state: Int64 {
        get() {
            2
        }
    }
}

class Child <: Parent {
    prop state: Int64 {
        get() {
            3
        }
    }
}
