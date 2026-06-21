

func f<T>() {
    var x: ??T = None
    x = None<T>
}

main() {
    println(f<String>())
}
