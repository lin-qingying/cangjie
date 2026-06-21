

main() {
    try {
    } catch (e: Exception) {
        let <!REDECLARATION!>e<!> = 0
        println(e)
    }
}
