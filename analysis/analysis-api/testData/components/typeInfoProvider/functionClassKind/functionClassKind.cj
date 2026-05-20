// FILE: functionClassKind.cj

package sample.typeinfo

func accepts(fn: (Int64) -> Int64): Int64 {
    return fn(1)
}

func use(callback: (Int64) -> Int64): Int64 {
    return accepts(<expr>callback</expr>)
}
