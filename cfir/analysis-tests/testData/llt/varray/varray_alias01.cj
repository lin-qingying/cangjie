

type varr = VArray<Int64, $3>
<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type varr1_1<T><!> = VArray<Int64, $3>

main() {
    var a: varr = varr(repeat: 0)
    var d = varr(repeat: 0)
    //   var b: varr1<Int64> = varr1<Int64>(repeat: 0)
    var c: varr1_1<Rune> = varr1_1<Rune>(repeat: 0)
    var e = varr1_1<Rune>(repeat: 0)
}
