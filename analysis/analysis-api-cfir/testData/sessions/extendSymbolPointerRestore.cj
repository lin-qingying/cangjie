package sample.sessions.extend

interface Printable {
    func prettyPrint(): Int64
}

class Document {
}

extend Document <: Printable {
    func prettyPrint(): Int64 {
        return 7
    }
}

func render(doc: Document): Int64 {
    return doc.prettyPrint()
}
