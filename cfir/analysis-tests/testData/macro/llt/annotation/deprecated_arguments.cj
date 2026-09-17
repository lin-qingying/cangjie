@Deprecated[message: <!DEPRECATED_WRONG_ARGUMENT!>1<!>]
func wrongMessage(): Unit {}

@Deprecated[message: <!DEPRECATED_EMPTY_STRING_ARGUMENT!>""<!>, since: <!DEPRECATED_EMPTY_STRING_ARGUMENT!>""<!>, strict: <!DEPRECATED_EMPTY_STRING_ARGUMENT!>""<!>]
func wrongValues(): Unit {}

@Deprecated[message: "first", message: <!DEPRECATED_ARGUMENT_DUPLICATION!>"second"<!>]
func duplicateMessage(): Unit {}

@Deprecated[<!DEPRECATED_UNKNOWN_ARGUMENT!>unknown: "value"<!>]
func unknownArgument(): Unit {}

func makeMessage(): String {
    return "message"
}

@Deprecated[<!DEPRECATED_ARGUMENTS_MUST_BE_LITERAL_CONST!>message: <!UNRESOLVED_REFERENCE!>makeMessage<!>()<!>]
func nonLiteralMessage(): Unit {}

func invalidParameters(<!DEPRECATED_INVALID_TARGET!>@Deprecated<!> parameter: Int64, @Deprecated namedParameter!: Int64 = 0): Unit {}

main() {}
