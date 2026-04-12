// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

// test ParseVArrayType.
main() {
    var a: VArray // error
    var a: VArray< // error
    var a: VArray<Int64 // error
    ()
    var a: VArray<Int64, // error
    ()
    var a: VArray<Int64, $ // error
    var a: VArray<Int64, $1 // error
    var a: VArray<Int64, $1> // ok

    var a: VArray
    <Int64, $1> // ok
    var a: VArray<
    Int64, $1> // ok
    var a: VArray<Int64
    , $1> // ok
    var a: VArray<Int64, 
    $1> // ok
    var a: VArray<Int64, $
    1> // error, cannot wrap line between '$' and literal.
    var a: VArray<Int64, $1
    > // ok
    0
}

/* SCAN
error: expected type parameters after 'VArray' keyword
  ==> varray15.cj:11:18:
   | 
11 |     var a: VArray // error
   |                  ^ 
   | 

error: expected type name after '<', found keyword 'var'
  ==> varray15.cj:13:5:
   | 
12 |     var a: VArray< // error
   |                  ~ after '<'
13 |     var a: VArray<Int64 // error
   |     ^^^ expected type name here
   | 

error: expected a type argument and size literal between '<' and '>' of 'VArray' type
  ==> varray15.cj:13:18:
   | 
13 |       var a: VArray<Int64 // error
   |  __________________^
14 | |     ()
   | |_____^
   | 

error: expected a '$' follows an integer literal as the second generic argument between '<' and '>' of 'VArray' type
  ==> varray15.cj:16:5:
   | 
16 |     ()
   |     ^ 
   | 

error: expected an integer literal than or equal to 0 after '$' to specificate the size of 'VArray' type
  ==> varray15.cj:17:26:
   | 
17 |     var a: VArray<Int64, $ // error
   |                          ^ 
   | 

error: unclosed delimiter: '<'
  ==> varray15.cj:18:28:
   | 
18 |     var a: VArray<Int64, $1 // error
   |                  ~         ^ expected '>' here
   |                  |
   |                  to match this opening '<'
   | 

error: expected an integer literal than or equal to 0 after '$' to specificate the size of 'VArray' type
  ==> varray15.cj:29:26:
   | 
29 |     var a: VArray<Int64, $
   |                          ^ 
   | 

error: expected expression after '>', found keyword 'var'
  ==> varray15.cj:31:5:
   | 
31 |     var a: VArray<Int64, $1
   |     ^^^ expected expression here
   | 

8 errors generated, 8 errors printed.
*/
