<!INVALID_INTRINSIC_DECL!>@Intrinsic
func declared(): Int64<!>

<!INVALID_INTRINSIC_DECL!>@Intrinsic
func withBody(): Int64 <!INTRINSIC_FUNCTION_CANNOT_HAVE_BODY!>{
    return 1
}<!><!>

class Host {
    <!INTRINSIC_FUNCTION_MUST_BE_TOPLEVEL, INVALID_INTRINSIC_DECL!>@Intrinsic
    func member(): Int64<!>
}

main() {}
