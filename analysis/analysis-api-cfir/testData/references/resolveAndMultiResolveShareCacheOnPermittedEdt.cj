package sample.references

func greet() {
}

func multiFirstCaller() {
    <caret_multiFirst>greet()
}

func singleFirstCaller() {
    <caret_singleFirst>greet()
}
