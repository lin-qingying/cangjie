package sample.scope

func generic<T>(value: T, second: T): T {
    let local = value
    scopeTarget
}

main() {
    generic<Int64>(1, 2)
}