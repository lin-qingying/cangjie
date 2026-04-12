// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

package p

enum E {
    | f(Int64)
    | f(Int64, Int64, Int64)
}
func f(a: Int64) {
    return a
}
func f(a: Int64, b: Int64) {
    return a + b
}


main() {
    let num1: Int64 = p.f(1) // ok
    let num
}
func f(a: Int64, b: Int64) {
    return a + b
}


main() {
    let num1: Int64 = p.f+ b
}


main() {
    let num1: Int64 = p.f(1) // ok
    let num
}
func f(a: Int64, b: Int64) {
    return a + b
}


main() {
    let num1: Int64 = p.(1) // ok
    let num2 = E.f(1) // ok
    let num3: Int64 = f(1, 2) // ok, find f(a: Int64, b: Int64)
    let num4 = f(1, 2, 3) // ok, find E.f(Int64, Int64,  _ => 0
    }

    return 0
}

/* SCAN
error: variable declaration 'num' needs either type or initializer
  ==> err_syntax_00.cj:25:12:
   | 
25 |     let num
   |     ~~~~~~~^ expected ':' or '=' after variable name
   | 

error: variable declaration 'num' needs either type or initializer
  ==> err_syntax_00.cj:39:12:
   | 
39 |     let num
   |     ~~~~~~~^ expected ':' or '=' after variable name
   | 

error: expected a member name after '.' in qualified name, found '('
  ==> err_syntax_00.cj:47:25:
   | 
47 |     let num1: Int64 = p.(1) // ok
   |                        ~^ expected a member name here
   |                        |
   |                        after '.' in qualified name
   | 

error: expected declaration, found keyword 'return'
  ==> err_syntax_00.cj:53:5:
   | 
53 |     return 0
   |     ^^^^^^ expected declaration here
   | 
   # note: only declarations or macro expressions can be used in the top-level

4 errors generated, 4 errors printed.
*/
