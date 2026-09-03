interface I1 { func foo(): Int }
interface I2 { func foo(): String }
class A<T> where T <: I2 {
    func test<X>() where X <: I1 & T {}
}
func f<T1, T2, T3>() where T1 <: T2 & I1, T2 <: T3, T3 <: I2 {}
main() {}
