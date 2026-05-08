open class Animal {}
class Dog <: Animal {}
struct Token {}

class Kennel<T> where T <: Animal & Dog {}

func crate<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>() where T <: Animal & Token {}
