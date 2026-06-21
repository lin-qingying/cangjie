


func Call(foo: CFunc<() -> Unit>): Unit {
    println("Call")
    unsafe { foo() }
}

var foo: CFunc<() -> Unit> = {=> println("foo")}

class A<T> {
    func x() {
        Call(foo)
    }
}

main() {
    let a = A<Int64>()

    a.x()
}
