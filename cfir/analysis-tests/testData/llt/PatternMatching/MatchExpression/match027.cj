

main() {
    let x = Some(true)
    match (x) {
        case Some(_) | Option.None => 0
    }
}
