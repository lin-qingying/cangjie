enum Token<T> { Missing | Value(T) }
type IntToken = Token<Int64>
type TokenAlias<T> = Token<T>

func identity<T>(value: Option<T>): Option<T> { value }
func tokenIdentity<T>(value: Token<T>): Token<T> { value }
func needInteger(value: Option<Int64>): Unit {}
func needToken(value: Token<Int64>): Unit {}

func typedOwner<T>(): Unit {
    let option: Option<T> = identity(Option<T>.None)
    let token: Token<T> = tokenIdentity(Token<T>.Missing)
}

main() {
    let integer: Option<Int64> = identity(Option<Int64>.None)
    let bottom: Option<Nothing> = identity(Option<Nothing>.None)
    let token: Token<Int64> = tokenIdentity(Token<Int64>.Missing)
    let alias: Token<Int64> = tokenIdentity(IntToken.Missing)
    let genericAlias: Token<Int64> = tokenIdentity(TokenAlias<Int64>.Missing)
    let bare: Option<Int64> = None
    needInteger(<!ARGUMENT_TYPE_MISMATCH!>Option<Bool>.None<!>)
    needToken(<!ARGUMENT_TYPE_MISMATCH!>Token<Bool>.Missing<!>)
    let invalidOption: Option<Int64> = <!TYPE_MISMATCH!>Option<Bool>.None<!>
    let invalidToken: Token<Int64> = <!TYPE_MISMATCH!>Token<Bool>.Missing<!>
}
