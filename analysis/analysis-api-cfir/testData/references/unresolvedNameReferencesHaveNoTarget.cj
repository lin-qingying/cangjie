package sample

func useUnresolved() {
    let value = missingValue
    missingFunction(1)
    let _ = value
}
