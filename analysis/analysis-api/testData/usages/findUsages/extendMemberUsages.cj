// TARGET_KIND: EXTEND_MEMBER
// TARGET_NAME: prettyPrint
// EXPECTED_USAGE_COUNT: 2
// EXPECTED_USAGE_SCOPE_KIND: ANY
package sample.findusages

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

func firstCaller(doc: Document) {
    doc.prettyPrint()
}

func secondCaller(doc: Document) {
    doc.prettyPrint()
}
