


func f(x: String) {
    match (x) {
        case "${x.size}" => 0
        case _ => 1
    }
}

main() {
    f("1")
}

