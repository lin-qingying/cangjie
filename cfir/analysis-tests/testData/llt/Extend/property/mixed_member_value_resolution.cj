class ValueOwner {
    public prop item: Int64 { get() { 1 } }
}
extend ValueOwner {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>item<!>(): Int64 { 2 }
}

class FunctionOwner {
    public func item(): Int64 { 1 }
}
extend FunctionOwner {
    public prop <!EXTEND_MEMBER_CANNOT_SHADOW!>item<!>: Int64 { get() { 2 } }
}

class HiddenValue {
    private prop item: Int64 { get() { 1 } }
}
extend HiddenValue {
    public func item(): Int64 { 2 }
}

class HiddenFunction {
    private func item(): Int64 { 1 }
}
extend HiddenFunction {
    public prop item: Int64 { get() { 2 } }
}

func declaredValue(): Int64 {
    let value = ValueOwner().item
    value
}

func declaredFunction(): Int64 {
    let fn = FunctionOwner().item
    fn()
}

func invisibleValue(): Int64 {
    let fn = HiddenValue().item
    fn()
}

func invisibleFunction(): Int64 {
    let value = HiddenFunction().item
    value
}

func targetFunction(): () -> Int64 {
    ValueOwner().item
}

func invalidTargets() {
    let value: Int64 = <!TYPE_MISMATCH!>ValueOwner().item<!>
    let fn: () -> Int64 = HiddenFunction().<!INVISIBLE_MEMBER!>item<!>
}

interface FunctionBound {
    func item(): Int64 { 1 }
}
interface PropertyBound {
    prop item: Int64 { get() { 1 } }
}
func boundMembers<T>(value: T) where T <: FunctionBound & PropertyBound {
    <!AMBIGUOUS_USE!>value.item<!>
    let fn: () -> Int64 = <!AMBIGUOUS_USE!>value.item<!>
    let number: Int64 = <!AMBIGUOUS_USE!>value.item<!>
}
