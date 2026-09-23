open class A {}
class B <: A {
    func f(): Unit {
        1 |> super
    }
}
main() {}
