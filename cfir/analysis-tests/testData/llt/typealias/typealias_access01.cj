internal class Hidden {}

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>A<!><!> = Hidden

internal type B = Hidden

public type C = Int64

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>D<!><!> = Option<Hidden>

public type E<T> = Array<T>

private type F = Hidden

private class PrivateTarget {}

<!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>K<!><!> = PrivateTarget

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>L<!><!> = (Hidden, Hidden)

internal class ChainTarget {}

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>PublicAlias<!><!> = ChainTarget

public type <!ACCESSIBILITY_ERROR!>PublicAliasUser<!> = PublicAlias

internal type InternalAlias = ChainTarget

public <!TYPEALIAS_EXTERNAL_REFER_INTERNAL!>type <!ACCESSIBILITY_ERROR!>InternalAliasUser<!><!> = InternalAlias

main() {
    return 0
}
