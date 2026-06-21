

package mod1.default

internal import mod1.p1.*

main(): Int64 {
    var a = foo(Int8(1))
    if (a != 11) {
        return 1
    }
    return 0
}

func goo() {
    return 2
}
