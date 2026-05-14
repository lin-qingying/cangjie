open class Animal {}
open class Vehicle {}
struct Token {}

class Kennel<<!CONFLICTING_UPPER_BOUNDS!>T<!>> where T <: Animal & Vehicle {}

func crate<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Animal & Token {}
