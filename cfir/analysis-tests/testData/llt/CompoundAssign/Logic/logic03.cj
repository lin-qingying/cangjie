


func f(a: Bool) {
    println("f")
    false
}

class A {
    mut prop a: Bool {
        get() {
            false
        }
        set(v) {
            ()
        }
    }
}

main() {
    var x = A()
    x.a &&= f(true)
}
