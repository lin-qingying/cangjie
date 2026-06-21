


public interface Tokens<T> {
    func dropFirst(): Tokens<T>
}

extend String <: Tokens<Rune> {
    <!RETURN_TYPE_INVARIANCE!>p<!>ublic func dropFirst(): String {
        if (this.size > 1) {
            this[1..this.size]
        } else {
            String()
        }
    }
}

main() {
    let parseRuneF: (Tokens<Rune>) -> Tokens<Rune> = {tokens => tokens.dropFirst()}

    let result = parseRuneF("Foo"[0..3])
}
