interface I64 {
    prop value: Int64 { get() { 1 } }
}

interface I32 {
    prop value: Int32 { get() { 2 } }
}

// 直接继承和扩展继承均在引入冲突的声明上报告，显式属性仍须满足每个接口的类型。
class <!INHERIT_MEMBER_TYPE_INCONSISTENT!>Direct<!> <: I64 & I32 {
    public prop <!PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF!>value<!>: Int64 { get() { 3 } }
}

class Extended {}
<!INHERIT_MEMBER_TYPE_INCONSISTENT!>extend<!> Extended <: I64 & I32 {
    public prop <!PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF!>value<!>: Int64 { get() { 3 } }
}

// 目标类型的既有属性成为实际实现时，类型错误仍属于该属性。
class Existing {
    public prop <!PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF!>value<!>: Int64 { get() { 3 } }
}
extend Existing <: I32 {}
