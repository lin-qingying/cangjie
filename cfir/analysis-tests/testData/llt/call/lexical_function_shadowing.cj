enum Cell<T> { Empty | Full(T) }

func choose(x: Int64): Int64 { x }
func noArguments(): Int64 { 1 }
func same(x: Int64): Int64 { x }
func generic<T>(x: T): Int64 { 1 }
func genericEnum<T>(x: Cell<T>): Int64 { 1 }
func unusedTypeParameter<T>(x: Int64): Int64 { x }
func staticTarget(): Int64 { 1 }
let seed: Int64 = 1

class StaticField {
    static let seed = seed
    static func value(): Int64 { seed }
}

class Host {
    public func choose(x: Bool): Bool { x }
    public func useMember(): Int64 { choose(1) }
    public func useMemberValue() {
        let action: (Int64) -> Int64 = choose
    }
}

class StaticOwner {
    static func staticTarget(): Bool { true }
    static func test(): Int64 { return <!RETURN_TYPE_MISMATCH!>staticTarget()<!> }
}

open class PrivateParent {
    private func choose(value: Bool): Bool { value }
    private func same(value: Int64): Bool { true }
}

class PrivateChild <: PrivateParent {
    func differentParameters(): Int64 { choose(1) }
    func identicalParameters(): Int64 { <!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>same<!>(1) }
}

func useLocal(): Int64 {
    func choose(x: Bool): Bool { x }
    return choose(1)
}

func useLocalArity(): Int64 {
    func noArguments(x: Bool): Bool { x }
    return noArguments()
}

func useNestedOverload(): Int64 {
    func choose(x: Bool): Bool { x }
    func choose(x: Int64): Int64 { x }
    return choose(1)
}

func sameSignature(): Int64 {
    func same(x: Int64): Bool { true }
    return <!RETURN_TYPE_MISMATCH!>same(1)<!>
}

func distinctGenericParameters(): Int64 {
    func generic<S>(x: S): Bool { true }
    return generic(1)
}

func matchingEnumParameters(): Int64 {
    func genericEnum<S>(x: Cell<S>): Bool { true }
    return <!RETURN_TYPE_MISMATCH!>genericEnum(Cell<Int64>.Empty)<!>
}

func genericArityAfterShadow(): Int64 {
    func unusedTypeParameter<U, V>(x: Int64): Bool { true }
    return <!GENERIC_ARGUMENT_NO_MATCH!>unusedTypeParameter<Int64>(1)<!>
}

main() { 0 }
