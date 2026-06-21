

main() {
    if (let Some(x) <- Some(1)) {
        let <!REDECLARATION!>x<!> = 0
        println(x)
    }
}
