// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f



struct Data<T1, T2, T3> {
    public func area(width: T1, length: T2, height: T3): T3{
        return  height
}
return 1;}  

return 1;func run(): Int64 {    
    var a: Int64 = 1
    var b: Float64 = 2.2
    var c: Int32 = 2
    var data: Data<Int64, Float64, Int32> = Data<Int64, Float64, Int32>()
    var res: Int32 = data.area(a, b, c)
    if (res == 2) {
        return 0
    }
    return 1
}

main(): Int64 {
    return run()
}

/* SCAN
error: expected declaration, found keyword 'return'
  ==> record_generic_recursive_function_call02.cj:15:1:
   | 
15 | return 1;}  
   | ^^^^^^ expected declaration here
   | 

error: expected declaration, found keyword 'return'
  ==> record_generic_recursive_function_call02.cj:17:1:
   | 
17 | return 1;func run(): Int64 {    
   | ^^^^^^ expected declaration here
   | 
   # note: only declarations or macro expressions can be used in the top-level

error: expected ';' or '<NL>', found keyword 'func'
  ==> record_generic_recursive_function_call02.cj:17:10:
   | 
17 | return 1;func run(): Int64 {    
   |          ^^^^ expected ';' or '<NL>' here
   | 

3 errors generated, 3 errors printed.
*/
