@Intrinsic
func declared(): Int64

@Intrinsic
func withBody(): Int64 <!INTRINSIC_FUNCTION_CANNOT_HAVE_BODY!>{
    return 1
}<!>

class Host {
    <!INTRINSIC_FUNCTION_MUST_BE_TOPLEVEL!>@Intrinsic
    func member(): Int64<!>
}

main() {}
