


func f(_: (Float32) -> Unit) {}

class A {}

main() {
    f(
        {
            _: Float32 =>
            func g<T0>() where T0 <: A {}
            g<A>()
        }
    )
}
