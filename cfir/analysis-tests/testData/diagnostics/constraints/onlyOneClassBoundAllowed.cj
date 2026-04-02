open class Animal {}
class Dog <: Animal {}
struct Token {}

class Kennel<T> where T <: Animal, T <: <!ONLY_ONE_CLASS_BOUND_ALLOWED!>Dog<!> {}

func crate<<!CONFLICTING_UPPER_BOUNDS!>T<!>>() where T <: Animal, T <: <!ONLY_ONE_CLASS_BOUND_ALLOWED!>Token<!> {}
