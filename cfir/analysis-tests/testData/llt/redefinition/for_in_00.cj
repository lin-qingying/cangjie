

main() {
    let xx = "unused"
    for (xx in [1, 2]) {
        println(xx)
        var <!REDECLARATION!>xx<!> = false
        println(xx)
    }
}
