package diagnostics

class Document {}

extend Document {
    func prettyPrint(): String {
        return "Document"
    }
}

func render(doc: Document): String {
    return doc.prettyPrint()
}
