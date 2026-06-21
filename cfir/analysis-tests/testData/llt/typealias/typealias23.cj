


// Test for generic instantiation correctness
class A<T> {}

type MMap<T> = Array<A<A<T>>>

func test<K>() {
    let v = MMap<K>()
}

main() {
    let k = test<Int64>()
    return 0
}
