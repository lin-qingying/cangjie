

interface I {}

extend Int8 <: I {}

func test<T>() where T <: I {
    1
}

func test<T>(a: Int64) {
    2
}

func foo(f: (Int64) -> Int64) {
    f(2)
}

main() {
    let v = foo(test<Float64>)
    if (v != 2) {
        return 1
    }
    return 0
}
