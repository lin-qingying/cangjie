open class Animal {}
open class Vehicle {}
struct Token {}

class Kennel<<!CONFLICTING_UPPER_BOUNDS!>T<!>> where T <: Animal, T <: <!ONLY_ONE_CLASS_BOUND_ALLOWED!>Vehicle<!> {}

func crate<<!CONFLICTING_UPPER_BOUNDS!>T<!>>() where T <: Animal, T <: <!ONLY_ONE_CLASS_BOUND_ALLOWED!>Token<!> {}
