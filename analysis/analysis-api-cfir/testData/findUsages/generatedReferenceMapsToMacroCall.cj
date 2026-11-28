package sample.findusages

func greet() {
}

func caller() {
    greet()
}

func generatedReferenceOrigin() {
    @Build(42)
}
