package sample.symbol.light

/**
 * Source class doc.
 */
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

/**
 * Source top level doc.
 */
func topLevel(): String {
    return "document"
}
