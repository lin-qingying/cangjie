


let f1 = {
    x => match (x) {
        case Some(v) => v + 1
        case NOne => 0
    }
}

main() {
    println(f1(Some(3)))
    println(f1(None))
    0
}
