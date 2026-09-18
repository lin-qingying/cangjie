// @C enum constructor payload is rejected by the official CFFI checker.
<!ILLEGAL_USE_OF_ANNOTATION!>@C<!>
enum NativeEnum {
    | <!ENUM_PATTERN_FUNC_CTYPE_ERROR!>Payload(Int64)<!>
}

@C
struct NativeRecord {}

// A non-C enum also rejects @C struct payload; String is the official exception.
enum RegularEnum {
    | <!ENUM_PATTERN_FUNC_CTYPE_ERROR!>Record(NativeRecord)<!>
    | Text(String)
}

main() {}
