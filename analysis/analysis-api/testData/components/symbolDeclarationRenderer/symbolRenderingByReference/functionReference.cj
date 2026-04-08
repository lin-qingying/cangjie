// FILE: functionReference.cj
// TARGET_NAME: greet
// EXPECTED_RENDERED_DECLARATION: func greet(value: Int64): Int64
package sample.symbols.render

func greet(value: Int64): Int64 {
    return value
}

func consume(): Int64 {
    return greet(1)
}
