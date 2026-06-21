

class A {
    var a = 1
}

class B {}

func test(a: (A) -> Int64) {
    0
}

func test(a: (B) -> Int64) {
    1
}

main() {
    test {r => r.a}
}
