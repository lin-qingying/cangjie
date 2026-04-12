// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class cls {
    static var a: Int64
    static init() {
        func f() {
            a = 1
        }
        f()
    }
}

main(): Int64 {
    var a: Int64
    func f() {
        a = 1
    }
    f()
    println(a)

    let b: Int64
    {=> b = 2}
    println(b)
    return 1
}

/* SCAN
error: the static member variable 'a' is not initialized
  ==> variable_use_before_init_07.cj:10:5:
   | 
10 |     static var a: Int64
   |     ^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: you may add an initializer inline or initialize the variable in the 'static init'
note: static init defined here
  ==> variable_use_before_init_07.cj:11:12:
   | 
11 |     static init() {
   |            ^^^^ 
   | 

error: cannot capture variable 'a' before initialization
  ==> variable_use_before_init_07.cj:22:9:
   | 
22 |         a = 1
   |         ^ 
   | 

error: variable 'a' is used before initialization
  ==> variable_use_before_init_07.cj:25:13:
   | 
25 |     println(a)
   |             ^ 
   | 

error: cannot capture variable 'b' before initialization
  ==> variable_use_before_init_07.cj:28:9:
   | 
28 |     {=> b = 2}
   |         ^ 
   | 

error: variable 'b' is used before initialization
  ==> variable_use_before_init_07.cj:29:13:
   | 
29 |     println(b)
   |             ^ 
   | 

5 errors generated, 5 errors printed.
*/
