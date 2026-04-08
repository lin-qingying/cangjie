package sample.extend.provider

interface Printable {
    func prettyPrint(): Int64
}

class Document {
}

extend Document <: Printable {
    func prettyPrint(): Int64 {
        return 42
    }
}
