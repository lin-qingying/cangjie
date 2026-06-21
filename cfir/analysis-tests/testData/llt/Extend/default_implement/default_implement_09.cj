

interface I {
    func test(): Int64 {
        1
    }
}

interface J {}

open class A {}

extend A <: I & J {}

class B <: A {}

main() {
    let b = B()
    b.test() - 1
}
