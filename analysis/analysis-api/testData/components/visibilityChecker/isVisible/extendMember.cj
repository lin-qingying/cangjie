// FILE: extendMember.cj
// TARGET_NAME: renderDocument
// VISIBILITY_TARGET_KIND: EXTEND_MEMBER
// EXPECTED_SYMBOL_VISIBILITY: INTERNAL
// EXPECTED_VISIBILITY_EXPLICIT: false
// EXPECTED_IS_VISIBLE: true
package sample.visibility

interface Printable {
    func renderDocument(): String
}

class Document {
}

extend Document <: Printable {
    func renderDocument(): String {
        return "document"
    }
}

func consume(doc: Document): String {
    return doc.renderDocument()
}
