

func f() {
    let x: Int64
    x = Some(1) ?? return 2
    return x
}

main() {
    f() - 1
}
