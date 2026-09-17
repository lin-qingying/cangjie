// CallingConv 仅接受全局 foreign / @C 函数；作用域检查独立于函数 ABI。
<!ONLY_CFUNC_CAN_USE_ANNOTATION!>@CallingConv[CDECL]<!>
func ordinaryFunction(): Unit {}

class Holder {
    <!ILLEGAL_SCOPE_USE_OF_ANNOTATION!>@CallingConv[CDECL]<!>
    func memberFunction(): Unit {}

    <!ILLEGAL_SCOPE_USE_OF_ANNOTATION!>@C<!>
    func memberCFunction(): Unit {}
}

func enclosing(): Unit {
    <!ILLEGAL_SCOPE_USE_OF_ANNOTATION!>@CallingConv[CDECL]<!>
    func localFunction(): Unit {}

    <!ILLEGAL_SCOPE_USE_OF_ANNOTATION!>@C<!>
    func localCFunction(): Unit {}
}

main() { 0 }
