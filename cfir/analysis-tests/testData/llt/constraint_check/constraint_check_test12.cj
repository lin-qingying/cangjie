

enum Option<T> {
    None
    | Some(T)
}

struct L<T> {}

func foo0<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>>(a: T) where T <: Array<T> {}

func foo1<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>>(a: T) where T <: (T, Int32) {}

func foo2<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>>(a: T) where T <: (T) -> Int32 {}

func foo3<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>>(a: T) where T <: (Int32) -> T {}

func foo4<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>>(a: T) where T <: L<T> {}

func foo5<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>>(a: T) where T <: Option<T> {}

func foo6<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>, U>(a: T, b: U) where T <: Option<U>, U <: Option<T> {}

func foo7<<!GENERIC_PARAM_EXIST_IN_CLASS_IRRELEVANT_UPPERBOUND_RECURSIVELY!>T<!>, U>(a: T, b: U) where T <: (U) -> Int32, U <: (T) -> Int32 {}

main(): Int64 {
    return 1
}
