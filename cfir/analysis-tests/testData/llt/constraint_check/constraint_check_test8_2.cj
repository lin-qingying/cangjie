

// UpperBound: rule 3: If there are multiple classes then these classes, they must be in one inheritance chain.

open class Base {}

class Child <: Base {}

func foo<T>() where T <: Base & Child {}

main(): Int64 {
    return 0
}
