// FILE: declarations.cj
package lowlevel.resolve

class <caret_1>Document {
    let title: String
    init(title: String) {
        this.title = title
    }
}

type <caret_2>TitleDoc = Document

let <caret_3>defaultTitle: String = "analysis"

func <caret_4>buildDocument(title: String): Document {
    return Document(title)
}
