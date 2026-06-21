


func app<T, R>(f: (T) -> R, arg: T) {
    f(arg)
}

func app(f: (String) -> String, arg: String) {
    f(arg)
}

main() {
    println([1, 2, 3][app({x => x + 1}, 1)])
}
