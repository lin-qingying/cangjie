// FILE: stringArgument.cj
package sample.expectedtype

func acceptText(text: String) {
    text
}

func useText() {
    acceptText(<caret>"hello")
}
