func name<T>(a:T):T{

return a
}


func test():Unit{
        let a:Int64 <!PATTERN_INITIALIZER_TYPE_MISMATCH!>=<!> <!TYPE_MISMATCH!>name(1.1)<!>
}