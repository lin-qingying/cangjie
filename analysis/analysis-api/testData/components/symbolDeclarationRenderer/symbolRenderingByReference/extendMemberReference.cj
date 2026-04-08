// FILE: extendMemberReference.cj
// TARGET_NAME: prettyPrint
// EXPECTED_RENDERED_DECLARATION: func prettyPrint(): String
package sample.symbols.render

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
