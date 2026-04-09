// FILE: substitutionFunction.cj
// TARGET_CLASS: FancyIntBox
// TARGET_NAME: convert
// OVERRIDE_TARGET_KIND: MEMBER_FUNCTION
// EXPECTED_ALL_OVERRIDDEN: sample.symbol.relation.overrides.IntBox.convert(value:Int64):Int64
// EXPECTED_ALL_OVERRIDDEN: sample.symbol.relation.overrides.Box.convert(value:T):T
// EXPECTED_DIRECT_OVERRIDDEN: sample.symbol.relation.overrides.IntBox.convert(value:Int64):Int64
package sample.symbol.relation.overrides

open class Box<T> {
    public func convert(value: T): T {
        return value
    }
}

open class IntBox <: Box<Int64> {
    public func convert(value: Int64): Int64 {
        return value
    }
}

class FancyIntBox <: IntBox {
    public func convert(value: Int64): Int64 {
        return value
    }
}
