


foreign func printS(s: S): Unit

@C
struct S {
    var a = VArray<VArray<VArray<VArray<Int32, $2>, $2>, $2>, $2>(repeat: [[[1, 2], [1, 2]], [[1, 2], [1, 2]]])
    var b = VArray<Int32, $3>(repeat: 3)
}

main() {
    var s: S = S()
    unsafe { printS(s) }
}
