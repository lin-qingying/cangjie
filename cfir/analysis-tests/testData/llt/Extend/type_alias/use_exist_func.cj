

class Foo {
    public func f() {
        1
    }
}

type F = Foo

interface I {
    func f() {
        2
    }
}

type i = I

extend F <: i {}

main() {
    var a = F()
    if (a.f() != 1) {
        return 1
    }
    return 0
}
