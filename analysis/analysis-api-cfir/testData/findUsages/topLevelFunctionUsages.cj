package sample.findusages

func greet() {
}

func firstCaller() {
    greet()
}

func secondCaller() {
    greet()
    greet()
}
