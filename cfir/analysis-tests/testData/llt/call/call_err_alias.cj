

type F = () -> Unit

main() {
    <!INVALID_CALLED_OBJECT!>F()<!>
}
