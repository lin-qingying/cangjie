

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

func name():Unit {
  <!CANNOT_INFER_PARAMETER_TYPE!>name1<!>(1)
   let a = A1(1)
   let b = A(1)
   let c:A <!PATTERN_INITIALIZER_TYPE_MISMATCH!>=<!> <!CANNOT_INFER_PARAMETER_TYPE!>None1<!>
    let d:A <!PATTERN_INITIALIZER_TYPE_MISMATCH!>=<!>None1<Int64>

    let e = None<Int64>
    let f:Option1<Int64> <!PATTERN_INITIALIZER_TYPE_MISMATCH!>=<!> None

      let g  = <!CANNOT_INFER_PARAMETER_TYPE!>Some2<!>(1)


}
