

interface IA {
    mut prop x: Int64
}

class A<T> where T <: IA {
    let t: T
    init(t: T) {
        this.t = t
    }
    func foo() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>t.x<!> = t.x + 1
    }
}

main() {}
