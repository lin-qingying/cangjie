// FILE: extendMemberReference.cj
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
    return doc.<caret>prettyPrint()
}
