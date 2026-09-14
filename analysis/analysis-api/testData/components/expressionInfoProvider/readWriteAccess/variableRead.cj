// FILE: variableRead.cj
// TARGET_NAME: counter
package sample.resolver.access

func consume(value: Int64): Int64 {
    return value
}

func readLocal(): Int64 {
    var counter: Int64 = 1
    return consume(counter)
}
