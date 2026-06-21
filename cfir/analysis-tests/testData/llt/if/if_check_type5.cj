


// The if expression does not have ending 'else' branch, so the entire type must always be the type of unit.
main() {
    let a = if (true) {
        1
    } else if (false) {
        1.0
    }
    // Check the type of 'a' is Unit.
    let b: Unit = a
    return 0
}
