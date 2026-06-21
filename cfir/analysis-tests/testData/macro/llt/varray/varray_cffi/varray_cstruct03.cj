


@C
struct S {
    var a: VArray<Int32, $3> = VArray<Int32, $3>(repeat: 1)
    var c: VArray<Int32, $2> = VArray<Int32, $2>(repeat: 2)
    var e: VArray<Int32, $0> = VArray<Int32, $0>(repeat: 4)
    var d: VArray<Int32, $4> = VArray<Int32, $4>(repeat: 3)
}

foreign func f(s: S): Int32

main() {
    var s = S()
    return unsafe { f(s) }
}
