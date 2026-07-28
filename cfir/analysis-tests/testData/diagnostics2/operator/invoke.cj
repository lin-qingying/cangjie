class B {}

class C {
    public operator func ()(): Unit {
        return
    }

    public operator func ()(a: Int64): Unit {
        return
    }
}

func testInvokeNoArgs(): Unit {
    let a = C()
    a()
}

func testInvokeWithArg(): Unit {
    let a = C()
    a(42)
}

func testNoInvoke(): Unit {
    let a = B()
    <!NO_MATCHING_OPERATOR_INVOKE!>a<!>()
}

class D {
    public operator func ()(x: Int64): Int64 {
        return x
    }
}

func testInvokeArgTypeMismatch(): Unit {
    let d = D()
    <!NO_MATCHING_OPERATOR_INVOKE!>d<!>(<!ARGUMENT_TYPE_MISMATCH!>1.0<!>)

}

class MultiInvoke {
    public operator func ()(): Int64 {
        return 0
    }

    public operator func ()(a: Int64): Int64 {
        return a
    }

    public operator func ()(a: Int64, b: Int64): Int64 {
        return a + b
    }
}

func testMultiInvokeOverloads(): Unit {
    let m = MultiInvoke()
    m()
    m(1)
    m(1, 2)
}

func testInvokeReturnValue(): Unit {
    let d = D()
    let result: Int64 = d(10)
}

func testInvokeReturnTypeMismatch(): Unit {
    let d = D()
    let _: Float64 = <!NO_MATCHING_OPERATOR_INVOKE, UNRESOLVED_REFERENCE!>d<!>(10)

}

struct S {
    public operator func ()(): Int64 {
        return 1
    }
}

func testStructInvoke(): Unit {
    let s = S()
    s()
}

struct EmptyStruct {}

func testStructNoInvoke(): Unit {
    let s = EmptyStruct()
    <!NO_MATCHING_OPERATOR_INVOKE!>s<!>()
}

open class Base {
    public operator func ()(): Unit {
        return
    }
}

class Derived <: Base {}

func testInheritedInvoke(): Unit {
    let d = Derived()
    d()
}
