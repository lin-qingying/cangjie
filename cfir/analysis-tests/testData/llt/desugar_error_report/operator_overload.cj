


open class A {
    public operator func -(input: A): Int64 {
        return 0
    }
}

main(): Int64 {
    var a: A = A()
    <!TYPE_INCOMPATIBLE!>a<!> -= a
    return 0
}
