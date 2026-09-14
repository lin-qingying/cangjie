// FILE: genericType.cj
package sample.typeprovider.refs

class Box<T> {
}

func makeBox(): <caret>Box<Int64> {
    return Box<Int64>()
}
