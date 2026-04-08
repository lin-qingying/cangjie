interface AA<T> {
    func compareTo(other: T): Int64 {
        return 0
    }
}

class BB <: AA<BB> {}

main(): Unit {
    let a = BB()
    a.compareTo(a)
}
