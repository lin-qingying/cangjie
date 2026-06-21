

interface I {
    static func f2(): Unit {
        println("f2")
    }
}

main() {
    I.f2() // OK
}
