

main() {
    let any: Any = 0
    if ((any is Int64) && (any is Any)) {
        0
    } else {
        1
    }
}
