


func Add<T>(a: T): T {
    return a
}

main() {
    var a: Float64 = 1.0
    var resa = Add<Float64>(a)
    var b: Int64 = 2
    var resb = Add<Int64>(b)
    var c: Float64 = 3.0
    var resc = Add<Float64>(c)
    var d: Int64 = 4
    var resd = Add<Int64>(d)
    var e: Bool = true
    var rese = Add<Bool>(e)
    if (resa == 1.0 && resb == 2 && resc == 3.0 && resd == 4 && rese == true) {
        return 0
    } else {
        return 1
    }
}
