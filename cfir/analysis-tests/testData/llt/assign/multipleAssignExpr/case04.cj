// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f

func test() {
    var a: String
    var b: Int64
    var c: String
    var d: Int64
    var f = {=> ("abce", (2, 3.0))}
    (a, (b, c)) = "TEST"
    (a, (b, c)) = f()
    (a, (b, c), d) = f()
    (a, (b, c, d)) = f()
    (a, b) = f()
}

main() {
}

/* SCAN
error: mismatched types
  ==> case04.cj:15:19:
   | 
15 |     (a, (b, c)) = "TEST"
   |     ~~~~~~~~~~~   ^^^^^^ the expression has type 'Struct-String'
   |     |
   |     can not assign 'Tuple<Struct-String, Tuple<Int64, Struct-String>>' with 'Struct-String'
   | 

error: mismatched types
  ==> case04.cj:16:19:
   | 
16 |     (a, (b, c)) = f()
   |             ~     ^^^ the expression has type 'Tuple<Struct-String, Tuple<Int64, Float64>>'
   |             |
   |             can not assign 'Struct-String' with 'Float64'
   | 

error: mismatched types
  ==> case04.cj:17:22:
   | 
17 |     (a, (b, c), d) = f()
   |     ~~~~~~~~~~~~~~   ^^^ the expression has type 'Tuple<Struct-String, Tuple<Int64, Float64>>'
   |     |
   |     the tuple has 3-elements, found one with 2-elements
   | 

error: mismatched types
  ==> case04.cj:18:22:
   | 
18 |     (a, (b, c, d)) = f()
   |         ~~~~~~~~~    ^^^ the expression has type 'Tuple<Struct-String, Tuple<Int64, Float64>>'
   |         |
   |         the tuple has 3-elements, found one with 2-elements
   | 

error: mismatched types
  ==> case04.cj:19:14:
   | 
19 |     (a, b) = f()
   |         ~    ^^^ the expression has type 'Tuple<Struct-String, Tuple<Int64, Float64>>'
   |         |
   |         can not assign 'Int64' with 'Tuple<Int64, Float64>'
   | 

5 errors generated, 5 errors printed.
*/
