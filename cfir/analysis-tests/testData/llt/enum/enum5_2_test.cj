

enum A {
    a
}

func test() {
    a
}

main() {
    let ret = match (test()) {
        case a => 0
    }
    if (ret == 0) {
        return 0
    }
    return 1
}
