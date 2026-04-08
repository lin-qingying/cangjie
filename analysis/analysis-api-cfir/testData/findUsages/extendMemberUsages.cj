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
