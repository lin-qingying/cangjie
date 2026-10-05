// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

public interface Printable {
    func render(): String
}

public class Document {
    public let title: String

    init(title: String) {
        this.title = title
    }
}

extend Document <: Printable {
    public func render(): String {
        return this.title
    }

    public func combine(other: Document): Document {
        return Document(this.title + other.title)
    }
}

// MODULE: main(lib)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.main

import sample.lib.Document

func renderDocument(doc: Document): String {
    return doc.<caret>render()
}