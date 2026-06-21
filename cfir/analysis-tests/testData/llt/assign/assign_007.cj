


class A {}

class B {}

public var (a, <!ACCESSIBILITY_WITH_MAIN_HINT!>b<!>): (Int64, A) = (1, A())

public var (<!ACCESSIBILITY_WITH_MAIN_HINT!>c<!>, d): (A, Int64) = (A(), 1)

public var (<!ACCESSIBILITY_WITH_MAIN_HINT!>a1<!>, <!ACCESSIBILITY_WITH_MAIN_HINT!>b1<!>): (B, A) = (B(), A())
