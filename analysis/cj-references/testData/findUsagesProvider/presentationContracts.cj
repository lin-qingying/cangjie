package sample.find.usages

import sample.provider.greet as welcome

class Base {
}

class Document<T> <: Base {
}

class Holder {
    prop state: Int64 {
        get() {
            1
        }
    }
}

interface Printable {
    func prettyPrint(): String
}

extend Document<Int64> <: Printable {
    func prettyPrint(): String {
        return greet(value: 1)
    }
}

let stored: Int64 = 1

func greet<R>(value: Int64): String {
    let captured = value
    return "value"
}
