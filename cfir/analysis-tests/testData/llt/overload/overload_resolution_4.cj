

class A {
    init(_: () -> Int64) {}
}

func f(_: A) {}

func f(_: Array<Bool>) {}

let _ = f(A() {=> 0})

let _ = f(true)

main() {}
