open class A {}
class B <: A {
    func f(): Unit {
        let x = 1
        x ~> super
    }
}
main() {}
