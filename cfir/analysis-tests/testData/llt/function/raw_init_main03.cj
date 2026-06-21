

class A {
    var `init` = 1
    prop <!REDECLARATION!>`init`<!>: Int64 {
        get() {
            1
        }
    }
}

main() {
}
