


func foo(x: Int32) {}

func foo(x: Int64) {}

func bar(x: Int64) {}

func bar(x: Int32) {}

func id<T>(x: T) {
    x
}

main() {
    foo(id(1))
    bar(id(1))
    ()
}
