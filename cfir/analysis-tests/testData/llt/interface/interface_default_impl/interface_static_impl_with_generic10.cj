

interface fatherInterface<T> {
    static func foo1(): Int8
    static func foo3(): Int8
    static func foo2(): Int8 {
        foo1()
    }
}

interface MyInterface<T> <: fatherInterface<T> {
    static func foo2(): Int8 {
        foo1() + foo2()
    } // infinite self recursive during run
}

interface myClass<T> <: MyInterface<T> {
    static func f(): Int8 {
        0
    }
    static func foo1(): Int8 {
        f()
    }
    static func foo3(): Int8 {
        1
    }
}

main() {
    myClass<Int32>.foo2()
    0
}
