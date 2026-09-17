// @C 的目标和泛型规则、FastNative 的 foreign 规则、Frozen 的局部限制各自诊断。
<!ILLEGAL_USE_OF_ANNOTATION!>@C<!>
class NotStruct {}

struct ExtendHolder {}

extend ExtendHolder {
    <!ILLEGAL_SCOPE_USE_OF_ANNOTATION!>@C<!>
    func memberCFunction(): Int64 { 0 }
}

@C
func genericFunction<!CFFI_CANNOT_HAVE_TYPE_PARAM!><T><!>(value: <!INVALID_CFUNC_PARAMETER_TYPE!>T<!>): Unit {}

<!ILLEGAL_USE_OF_ANNOTATION!>@FastNative<!>
func notForeign(): Unit {}

<!ILLEGAL_USE_OF_ANNOTATION!>@Frozen<!>
struct NotFunctionOrProperty {}

func enclosing(): Unit {
    <!ILLEGAL_USE_OF_ANNOTATION!>@Frozen<!>
    func localFrozen(): Unit {}
}

main() { 0 }
