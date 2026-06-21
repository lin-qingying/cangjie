

main() {
    let x = (true, true)
    match (x) {
        case (true, true) | (false, false) => 0
        case (true, false) | (false, true) => 1
    }
}
