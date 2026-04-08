package sample.cdoc

class Document {
}

/**
 * Greets the caller.
 * @param [value] input value
 * @return rendered text
 * @see [Document]
 */
func describe(value: Int64): String {
    return "document"
}

func consume() {
    describe(1)
}
