// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    let a1: VArray = [1, 2, 3] // error
    let a2: VArray<$1> = [1, 2, 3] //error
    let a3: VArray<Int64> = [1, 2, 3] //error
    let a4: VArray<$3, Int64> = [1, 2, 3] //error
    let a5: VArray<$3, Int64, Int64> = [1, 2, 3] //error
    let a6: VArray<Int64, Int64> = [1, 2, 3] //error
    0
}

/* SCAN
error: expected type parameters after 'VArray' keyword
  ==> varray02.cj:10:19:
   | 
10 |     let a1: VArray = [1, 2, 3] // error
   |                   ^ 
   | 

error: expected type name after '<', found '$'
  ==> varray02.cj:11:20:
   | 
11 |     let a2: VArray<$1> = [1, 2, 3] //error
   |                   ~^ expected type name here
   |                   |
   |                   after '<'
   | 

error: expected a type argument and size literal between '<' and '>' of 'VArray' type
  ==> varray02.cj:12:19:
   | 
12 |     let a3: VArray<Int64> = [1, 2, 3] //error
   |                   ^^^^^^^ 
   | 

error: expected type name after '<', found '$'
  ==> varray02.cj:13:20:
   | 
13 |     let a4: VArray<$3, Int64> = [1, 2, 3] //error
   |                   ~^ expected type name here
   |                   |
   |                   after '<'
   | 

error: expected type name after '<', found '$'
  ==> varray02.cj:14:20:
   | 
14 |     let a5: VArray<$3, Int64, Int64> = [1, 2, 3] //error
   |                   ~^ expected type name here
   |                   |
   |                   after '<'
   | 

error: expected a '$' follows an integer literal as the second generic argument between '<' and '>' of 'VArray' type
  ==> varray02.cj:15:27:
   | 
15 |     let a6: VArray<Int64, Int64> = [1, 2, 3] //error
   |                           ^^^^^ 
   | 

6 errors generated, 6 errors printed.
*/
