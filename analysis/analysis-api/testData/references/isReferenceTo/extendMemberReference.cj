// FILE: extendMemberReference.cj
// TARGET_KIND: EXTEND_MEMBER
// TARGET_NAME: prettyPrint
// EXPECTED_TARGET_CLASS: CjNamedFunction
package sample.references.behavior

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

func consume(doc: Document): String {
    return doc.prettyPrint()
}
