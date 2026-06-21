

// Test condition: package with a single generic function is all imported by using "internal import with.pkgName.*"

internal import pkgs004001a.*

main(): Int64 {
    var a: Float64 = 1.0
    var resa = Foo<Float64>(a)
    var b: Int64 = 2
    var resb = Foo<Int64>(b)
    var c: Float64 = 3.0
    var resc = Foo<Float64>(c)
    var d: Int64 = 4
    var resd = Foo2<Int64, Bool>(d, false)
    var e: Bool = true
    var rese = Foo<Bool>(e)
    var f: Int32 = 5
    var resf = Bar<Int32>(f)
    if (resa == 1.0 && resb == 2 && resc == 3.0 && resd == 4 && rese == true && resf == 5) {
        return 0
    } else {
        return 1
    }
}
