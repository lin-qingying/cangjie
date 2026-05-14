// FILE: functionReference.cj
package sample.symbols.render

func greet(value: Int64): Int64 {
    return value
}

func consume(): Int64 {
    return <caret>greet(1)
}
