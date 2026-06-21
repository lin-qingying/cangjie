


open class A {
    var a_ = 0
    public mut open prop a: Int64 {
        get() {
            a_
        }
        set(v) {
            a_ = v
        }
    }
}

class B <: A {
    public mut override prop a: Int64 {
        set(v) {}
    }
}

class C <: A {
    public mut override prop a: Int64 {
        get() {
            a_ + 1
        }
    }
}

main() {
    let b = B()
    b.a++
    if (b.a != 0) {
        return 1
    }
    let c = C()
    c.a++
    if (c.a != 3) {
        return 2
    }
    return 0
}
