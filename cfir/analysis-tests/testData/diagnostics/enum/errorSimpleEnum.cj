enum Option1<T>{
    Some1(T) |
    None1
}

enum A{

    A1 |
    A1(Int64) |
    B

}
func name1<T>(a:Int64):Unit {

}

enum Option2<T>{
    Some2(Int64) |
    None2
}
func identity<T>(value: Int64): T {
    return <!RETURN_TYPE_MISMATCH!>value<!>
}
func identity1<T>(value: T): T {
    return  value
}

func name():Unit {
  <!UNABLE_TO_INFER_GENERIC_FUNC!>name1<!>(1)
   let a = A1(1)

   let b = <!NO_MATCHING_OPERATOR_INVOKE!>A<!>(1)
   let c:A = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>None1<!>
    let d:A =<!TYPE_MISMATCH!>None1<Int64><!>

    let e = None<Int64>
    let fOption1:Option1<Int64> = <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>None<!>

      let gSome2  = <!UNABLE_TO_INFER_GENERIC_FUNC!>Some2<!>(1)


    let fStd = Some<Float64>(<!CANNOT_CONVERT_LITERAL!>1<!>)
    let gStd = None<Int64>

}
