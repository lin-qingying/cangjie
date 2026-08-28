


interface IA {
    mut func f1(): Unit
    func f2(): Unit
}

struct A <: IA {
    public mut func f1(): Unit {
        println("f1")
    }
    public func f2(): Unit {
        println("f2")
    }
}

func goo(a: A): Unit {
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>a<!>.f1()
    a.f2()
}

func foo<T>(t: T): Unit where T <: IA {
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>t<!>.f1()
    t.f2()
}
