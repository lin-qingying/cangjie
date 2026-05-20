package sample.reference.utils

class Base(label: Float64) {
}

class Document <: Base(label: 1.0) {
}

/**
 * @see [Document]
 */
func helper(label: Int64): Document {
    return Document()
}

func consume(): Document {
    let result: Document = helper(label: 1)
    return result
}
