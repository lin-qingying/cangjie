

struct S {
    var i = 0
    mut func f() {
        i += 1
    }
}

class C {
    var a = S()
}

func f() {
    C()
}

main() {
    ((f()).a).f()
}
