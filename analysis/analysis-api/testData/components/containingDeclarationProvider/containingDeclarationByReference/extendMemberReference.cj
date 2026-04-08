// FILE: extendMemberReference.cj
// TARGET_NAME: prettyPrint
// EXPECTED_CONTAINING_DECLARATION: sample.containing.reference.Document.prettyPrint
// EXPECTED_CONTAINING_DECLARATION: sample.containing.reference:Document<:Printable
package sample.containing.reference

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
