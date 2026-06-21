

class C {
    var c = 0
}

struct A {
    let x = [0, 1]
    var c = C()

    func f() {
        x[0] += 1
        c.c += 1
    }
}

main() {
    let a = A()
    a.f()
    a.x[0] + a.c.c
}
