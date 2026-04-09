// MODULE: main
// MAIN_MODULE
// MAIN_FILE_NAME: functionChain.cj
// FILE: base.cj
package sample.symbol.relation.overrides

open class Root {
    public func render(value: Int64): Int64 {
        return value
    }
}

open class Parent <: Root {
    public func render(value: Int64): Int64 {
        return value
    }
}

// FILE: functionChain.cj
// TARGET_CLASS: Child
// TARGET_NAME: render
// OVERRIDE_TARGET_KIND: MEMBER_FUNCTION
// EXPECTED_ALL_OVERRIDDEN: sample.symbol.relation.overrides.Parent.render(value:Int64):Int64
// EXPECTED_ALL_OVERRIDDEN: sample.symbol.relation.overrides.Root.render(value:Int64):Int64
// EXPECTED_DIRECT_OVERRIDDEN: sample.symbol.relation.overrides.Parent.render(value:Int64):Int64
package sample.symbol.relation.overrides

class Child <: Parent {
    public func render(value: Int64): Int64 {
        return value
    }
}
