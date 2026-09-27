package k08

enum E {
    | common | specific
}

main(): Int64 {
    let e = common
    match (e) {
        case common => 1
        case specific => 2
    }
}
