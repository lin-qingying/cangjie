func name<T>(a:T):T{

return a
}


func test():Unit{
        let a:Int64 = <!UNABLE_TO_INFER_GENERIC_FUNC!>name<!>(1.1)
}
