struct Sample {
    public func mono(a: Int64): Int64 { a }
}

extend Sample {
    public func overloaded(a: Int64): Int64 { a }
    public func overloaded(a: Bool): Int64 { if (a) { 1 } else { 0 } }
}

func plain(a: Int64): Int64 { a }
func plain(a: Bool): Int64 { if (a) { 1 } else { 0 } }

let s = Sample()

// 右侧函数引用由组合的参数类型选定；整体类型不能消除左侧独立引用的歧义。
let rightMember = s.mono ~> s.overloaded
let rightTopLevel: (Int64) -> Int64 = s.mono ~> plain
let bothOverloaded = s.<!AMBIGUOUS_USE!>overloaded<!> ~> s.overloaded
let leftOverloaded = s.<!AMBIGUOUS_USE!>overloaded<!> ~> s.mono
let typedLeft: (Int64) -> Int64 = s.<!AMBIGUOUS_USE!>overloaded<!> ~> s.overloaded
let plainBoth = <!AMBIGUOUS_USE!>plain<!> ~> plain

// flow 上下文不穿透括号进入内部函数引用。
let parenthesizedRight = s.mono ~> (s.<!AMBIGUOUS_USE!>overloaded<!>)

// 两侧独立根错误均保留，错误操作数不再触发合成 composition 调用诊断。
let bothMissing = <!UNRESOLVED_REFERENCE!>missingLeft<!> ~> <!UNRESOLVED_REFERENCE!>missingRight<!>
let ambiguousAndMissing = s.<!AMBIGUOUS_USE!>overloaded<!> ~> <!UNRESOLVED_REFERENCE!>missingRight<!>
let missingAndOverloaded = <!UNRESOLVED_REFERENCE!>missingLeft<!> ~> s.overloaded
let missingReceiver = <!UNRESOLVED_REFERENCE!>unknownReceiver<!>.overloaded ~> s.overloaded

func invokeErroneousComposition() {
    bothMissing(1)
}
