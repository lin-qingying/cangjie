// FILE: substitutionFunction.cj
// TARGET_NAME: convert
// OVERRIDE_TARGET_KIND: MEMBER_FUNCTION
// EXPECTED_ALL_OVERRIDDEN: sample.symbol.relation.overrides.StringBox.convert(value: String): String
// EXPECTED_ALL_OVERRIDDEN: sample.symbol.relation.overrides.Box.convert(value: T): T
// EXPECTED_DIRECT_OVERRIDDEN: sample.symbol.relation.overrides.StringBox.convert(value: String): String
package sample.symbol.relation.overrides

open class Box<T> {
    public func convert(value: T): T {
        return value
    }
}

open class StringBox <: Box<String> {
    public func convert(value: String): String {
        return value
    }
}

class FancyStringBox <: StringBox {
    public func convert(value: String): String {
        return value
    }
}
