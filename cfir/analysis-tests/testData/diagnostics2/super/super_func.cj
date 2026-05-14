interface AA<T> {
    func compareTo(other: T): Int64 {
        let _ = other
        return 0
    }
}

class BB <: AA<BB> {}

main(): Unit {
    let a = BB()
    a.compareTo(a)
}
