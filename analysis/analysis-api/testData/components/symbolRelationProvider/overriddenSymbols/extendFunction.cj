// FILE: extendFunction.cj
// TARGET_NAME: prettyPrint
// OVERRIDE_TARGET_KIND: EXTEND_FUNCTION
// EXPECTED_ALL_OVERRIDDEN: sample.symbol.relation.overrides.Printable.prettyPrint(): String
// EXPECTED_DIRECT_OVERRIDDEN: sample.symbol.relation.overrides.Printable.prettyPrint(): String
package sample.symbol.relation.overrides

interface Printable {
    func prettyPrint(): String
}

class Document {
}

extend Document <: Printable {
    func prettyPrint(): String {
        return "document"
    }
}
