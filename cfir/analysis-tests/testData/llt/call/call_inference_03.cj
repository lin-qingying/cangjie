

main(): Unit {
    f2(1) {v => [v]}
}

func f2<V, R>(value: V, transform: (V) -> R): R {
    transform(value)
}
