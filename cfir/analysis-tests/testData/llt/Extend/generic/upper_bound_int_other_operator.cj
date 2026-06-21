

interface I {}

extend Int8 <: I {}

class C<T> where T <: I {
    func f(a: Int8) {
        var b = a
        b += 1
        b++
        return b
    }
}

main() {
    var c = C<Int8>()
    print(c.f(1))
    if (c.f(1) != 3) {
        return 1
    }
    return 0
}
