
class B {}

class C {
    public operator func ()(): Unit {
        return
    }

    public operator func ()(a: Int64): Unit {
        return
    }
}

// ---- 基本场景：有 invoke 操作符的类型 ----

func testInvokeNoArgs(): Unit {
    let a = C()
    a()  // OK：匹配 operator func ()(): Unit
}

func testInvokeWithArg(): Unit {
    let a = C()
    a(42)  // OK：匹配 operator func ()(a: Int64): Unit
}

// ---- 基本场景：无 invoke 操作符的类型 ----

func testNoInvoke(): Unit {
    let a = B()
    <!NO_MATCHING_OPERATOR_INVOKE!>a<!>()  // 错误：B 没有定义 operator func ()()
}

// ---- 参数类型不匹配 ----

class D {
    public operator func ()(x: Int64): Int64 {
        return x
    }
}

func testInvokeArgTypeMismatch(): Unit {
    let d = D()
    <!NO_MATCHING_OPERATOR_INVOKE!>d<!>(<!ARGUMENT_TYPE_MISMATCH!>1.0<!>)  // 错误：Float64 不匹配 Int64
}

// ---- 多个重载 ----

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
    m()       // OK：匹配无参版本
    m(1)      // OK：匹配单参版本
    m(1, 2)   // OK：匹配双参版本
}

// ---- 有返回值的 invoke ----

func testInvokeReturnValue(): Unit {
    let d = D()
    d(10)  // OK：返回 Int64
}

func testInvokeReturnTypeMismatch(): Unit {
    let d = D()
    let _: Float64 = <!TYPE_MISMATCH!>d(10)<!>  // 错误：invoke 返回值与期望类型不匹配
}

// ---- struct 上定义 invoke ----

struct S {
    public operator func ()(): Int64 {
        return 1
    }
}

func testStructInvoke(): Unit {
    let s = S()
    s()  // OK：struct 也可以定义 invoke
}

struct EmptyStruct {}

func testStructNoInvoke(): Unit {
    let s = EmptyStruct()
    <!NO_MATCHING_OPERATOR_INVOKE!>s<!>()  // 错误：EmptyStruct 没有 invoke
}

// ---- 通过继承获得 invoke ----

open class Base {
    public operator func ()(): Unit {
        return
    }
}

class Derived <: Base {}

func testInheritedInvoke(): Unit {
    let d = Derived()
    d()  // OK：继承自 Base 的 invoke
}

// ---- invoke 与普通函数同名场景 ----

class WithBoth {
    public operator func ()(): Unit {
        return
    }

    public func call(): Unit {
        return
    }
}

func testInvokeAndMethod(): Unit {
    let w = WithBoth()
    w()       // OK：调用 invoke
    w.call()  // OK：调用普通方法
}
