package sample.references

class Document {
}

interface Printable {
    func prettyPrint(): String
}

extend Document <: Printable {
    func prettyPrint(): String {
        return "document"
    }
}
