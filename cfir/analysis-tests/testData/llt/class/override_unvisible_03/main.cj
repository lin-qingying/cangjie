// FILE: pkg.cj


package pkg

public open class A {
    func f() {
        0
    }
}

public open class B <: A {
    func ff() {
        2
    } // f2, override f1
}

extend B {
    func fff() {
        2
    }
}

// FILE: main.cj



internal import pkg.*

extend B {
    func f() {
        3
    }
    func ff() {
        3
    } // f3. not protected override function for f2/f1
    func fff() {
        3
    }
}

main() {
    let v = B().ff() - B().fff()
    return v
}
