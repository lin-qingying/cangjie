// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    let a1: VArray = VArray<Int64, $3> {i => i} // x2 error
    let a2: VArray<Int64> = VArray<Int64, $3> {i => i} // x2 error
    let a3: VArray<$3> = VArray<Int64, $3> {i => i} // x2 error

    let a4: VArray<Int64, $3> = VArray {i => i} // x2 error
    let a5: VArray<Int64, $3> = VArray<Int64> {i => i} // x2 error
    let a6: VArray<Int64, $3> = VArray<$3> {i => i} // x2 error
    let a7: VArray<Int64, $3> = VArray<Int64, $3>({i => i} // error
    0
}

/* SCAN
error: expected type parameters after 'VArray' keyword
  ==> varray08.cj:10:19:
   | 
10 |     let a1: VArray = VArray<Int64, $3> {i => i} // x2 error
   |                   ^ 
   | 

error: expected a type argument and size literal between '<' and '>' of 'VArray' type
  ==> varray08.cj:11:19:
   | 
11 |     let a2: VArray<Int64> = VArray<Int64, $3> {i => i} // x2 error
   |                   ^^^^^^^ 
   | 

error: expected type name after '<', found '$'
  ==> varray08.cj:12:20:
   | 
12 |     let a3: VArray<$3> = VArray<Int64, $3> {i => i} // x2 error
   |                   ~^ expected type name here
   |                   |
   |                   after '<'
   | 

error: expected type parameters after 'VArray' keyword
  ==> varray08.cj:14:39:
   | 
14 |     let a4: VArray<Int64, $3> = VArray {i => i} // x2 error
   |                                       ^ 
   | 

error: expected a type argument and size literal between '<' and '>' of 'VArray' type
  ==> varray08.cj:15:39:
   | 
15 |     let a5: VArray<Int64, $3> = VArray<Int64> {i => i} // x2 error
   |                                       ^^^^^^^ 
   | 

error: expected type name after '<', found '$'
  ==> varray08.cj:16:40:
   | 
16 |     let a6: VArray<Int64, $3> = VArray<$3> {i => i} // x2 error
   |                                       ~^ expected type name here
   |                                       |
   |                                       after '<'
   | 

error: unclosed delimiter: '('
  ==> varray08.cj:17:59:
   | 
17 |     let a7: VArray<Int64, $3> = VArray<Int64, $3>({i => i} // error
   |                                                  ~        ^ expected ')' here
   |                                                  |
   |                                                  to match this opening '('
   | 

7 errors generated, 7 errors printed.
*/
