// FILE: extendFunction.cj
// TARGET_NAME: prettyPrint
// OVERRIDE_TARGET_KIND: EXTEND_FUNCTION
// EXPECTED_ALL_OVERRIDDEN: sample.symbol.relation.overrides.Printable.prettyPrint():Int64
// EXPECTED_DIRECT_OVERRIDDEN: sample.symbol.relation.overrides.Printable.prettyPrint():Int64
package sample.symbol.relation.overrides

interface Printable {
    func prettyPrint(): Int64
}

class Document {
}

extend Document <: Printable {
    func prettyPrint(): Int64 {
        return 1
    }
}
