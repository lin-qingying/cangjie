


// The if expression does not have ending 'else' branch, so the entire type must always be the type of unit.
main() {
    var a: Unit = if (true) {
        1
    } else if (false) {
        1.0
    }
    return 0
}
