// 声明类型参数不能由标量字面量构造；显式返回与 getter 使用相同的转换诊断。
func intTail<T>(): T { <!CANNOT_CONVERT_LITERAL!>1<!> }
func floatTail<T>(): T { <!CANNOT_CONVERT_LITERAL!>1.0<!> }
func boolTail<T>(): T { <!CANNOT_CONVERT_LITERAL!>true<!> }
func runeTail<T>(): T { <!CANNOT_CONVERT_LITERAL!>r'a'<!> }
func byteTail<T>(): T { <!CANNOT_CONVERT_LITERAL!>b'a'<!> }
func stringTail<T>(): T { <!RETURN_TYPE_MISMATCH!>"a"<!> }
func intReturn<T>(): T { return <!CANNOT_CONVERT_LITERAL!>1<!> }

interface Holder<T> {
    prop intValue: T { get() { <!CANNOT_CONVERT_LITERAL!>1<!> } }
    prop floatValue: T { get() { <!CANNOT_CONVERT_LITERAL!>1.0<!> } }
    prop boolValue: T { get() { <!CANNOT_CONVERT_LITERAL!>true<!> } }
    prop runeValue: T { get() { <!CANNOT_CONVERT_LITERAL!>r'a'<!> } }
    prop byteValue: T { get() { <!CANNOT_CONVERT_LITERAL!>b'a'<!> } }
    prop stringValue: T { get() { <!RETURN_TYPE_MISMATCH!>"a"<!> } }
}

// 推断变量和 Any 仍允许从字面量获得具体类型。
func arguments<T>(a: T): T { a }
func inference() {
    let a = arguments(1)
    let b = arguments(true)
}
func boxInteger(): Any { 1 }
func boxFloat(): Any { 1.0 }
func boxBoolean(): Any { true }
