

open class A<T> {
    let x: T
    init(x: T) {
        this.x = x
    }
}

class B <: A<Int64> {
    let y = <!USED_BEFORE_INITIALIZATION!>x<!>
    init(x: Int64) {
        super(x)
    }
}

main() {
    println(B(10).y)
}
