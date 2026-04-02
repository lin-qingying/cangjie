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
  <!CANNOT_INFER_PARAMETER_TYPE!>name1<!>(1)
   let a = A1(1)

   let b = <!NO_CONSTRUCTOR!>A<!>(1)
   let c:A = <!CANNOT_INFER_PARAMETER_TYPE!>None1<!>
    let d:A =<!TYPE_MISMATCH!>None1<Int64><!>

    let e = None<Int64>
    let f:Option1<Int64> = <!CANNOT_INFER_PARAMETER_TYPE!>None<!>

      let g  = <!CANNOT_INFER_PARAMETER_TYPE!>Some2<!>(1)


    let f = Some<Float64>(<!ARGUMENT_TYPE_MISMATCH!>1<!>)
    let g = None<Int64>

}
