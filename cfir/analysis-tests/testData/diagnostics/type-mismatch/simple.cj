func name<T>(a:T):T{

return a
}


func test():Unit{
        let a:Int64 = <!NEW_INFERENCE_ERROR!>name<!>(1.1)
}
