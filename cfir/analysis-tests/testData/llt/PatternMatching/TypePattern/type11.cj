

main() {
    let text: Any = "123"
    match (text) {
        case _: Option<String> => println("option string")
        case _: String => println("string")
        case _ => println("other type")
    }
}
