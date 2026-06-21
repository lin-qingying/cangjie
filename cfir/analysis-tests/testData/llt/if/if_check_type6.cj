


// The if expression does not have ending 'else' branch, so the entire type must always be the type of unit.
main() {
    let t = if (let Some(_) <- Some(0)) {
        1
    } else if (true) {
        2.0
    }
    let b: Unit = t
}
