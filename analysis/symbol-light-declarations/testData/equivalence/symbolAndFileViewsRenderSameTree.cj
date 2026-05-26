package sample.symbol.light.equivalence

class Document {
    func member(): Int64 {
        return 1
    }
}

type DocAlias = Document

interface Printable {
    func prettyPrint(): String
}

extend Document <: Printable {
    func prettyPrint(): String {
        return topLevel()
    }
}

func topLevel(): String {
    return "document"
}
