package sample.lowlevel.lazy.scopes

interface Printable {
    func render(): String
}

open class Document <: Printable {
    public func render(): String {
        return "document"
    }
}

class <caret>PrettyDocument <: Document {
}
