

main(): Int64 {
    return run()
}

var x: Int64 = 2
let y: String = "yz"

func run(): Int64 {
    var res = Foo(1, "x")
    if (res == "3xyz") {
        return 0
    }
    return 1
}

func Foo(a: Int64, b: String, A!: Int64 = x, B!: String = Foo(1, "x")): String {
    return (a + A).toString() + b + B
}
