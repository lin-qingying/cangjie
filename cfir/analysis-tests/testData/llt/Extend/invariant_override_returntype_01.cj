


public interface Tokens<T> {
    func dropFirst(): Tokens<T>
}

extend String <: Tokens<Rune> {
    public func <!RETURN_TYPE_INVARIANCE!>dropFirst<!>(): String {
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
