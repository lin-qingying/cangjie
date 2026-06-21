

main() {
    let x = Some(true)
    match (x) {
        case _: Int64 | _: Option<Bool> => 0
        case Some(false) => 1
        case _ => 2
    }
}
