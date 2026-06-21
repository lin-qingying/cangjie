


interface I {}

class A {}

class B <: I {}

func test<T>(a: T) where T <: I {}

func test<T>(a: T, b!: Int64 = 1) {}

main() {
    let a: (A, Int64) -> Unit = test<A>
    0
}
