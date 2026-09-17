// @C struct 字段必须满足 CType；Unit 使用独立诊断，范围为字段名及显式类型。
struct PlainRecord {
    var value: Int64 = 0
}

@C
struct NativeRecord {
    var <!CSTRUCT_CANNOT_HAVE_UNIT_FIELDS!>unitField: Unit<!> = ()
    var <!ILLEGAL_MEMBER_OF_CSTRUCT!>stringField: String<!> = ""
    var <!ILLEGAL_MEMBER_OF_CSTRUCT!>runeField: Rune<!> = r'a'
    var <!ILLEGAL_MEMBER_OF_CSTRUCT!>optionField: ?Int64<!> = None
    var <!ILLEGAL_MEMBER_OF_CSTRUCT!>plainField: PlainRecord<!> = PlainRecord()
    var <!ILLEGAL_MEMBER_OF_CSTRUCT!>callbackField: (Int64) -> Unit<!> = { value => () }
}

@C
struct CapturedCStruct {
    var value: Int32 = 0
}

func captureCStruct(): Int32 {
    let value = CapturedCStruct()
    let closure = { => <!FUNC_CAPTURE_VAR_NOT_CTYPE!>value<!> }
    closure()
    0
}

main() { 0 }
