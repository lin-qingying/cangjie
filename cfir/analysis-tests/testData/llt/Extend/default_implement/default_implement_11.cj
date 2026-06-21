

interface I1 {
    func foo() {
        println("foo")
    }
}

interface I2 <: I1 {}

class A {}

extend A <: I1 {}

extend A <: I2 {} // A 中已存在 foo，实现子接口的扩展不再拷贝 foo 。

class B {}

extend B <: I2 {}

extend B <: I1 {}

main() {
    A().foo()
    B().foo()
}
