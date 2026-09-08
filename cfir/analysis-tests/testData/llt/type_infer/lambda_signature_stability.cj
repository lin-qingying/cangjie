let stringify = { x => x.toString() }
main() {
    println(stringify(Some(1)))
    println(stringify("text"))
    println(stringify(true))
    return 0
}
