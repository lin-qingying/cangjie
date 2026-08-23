


// DIAGNOSTICS: -UNUSED_EXPRESSION

main() {
    let any: Any = 0
    let x = any as Any
    match (x) {
        case Some(_) => 0
        case None => 1
    }
}

