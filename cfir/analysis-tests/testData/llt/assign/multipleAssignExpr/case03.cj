// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f

func test() {
    var a = 1
    var b = 2
    var c = 3
    (a, d, e) = (1, 2, 3)
    (a, b, c, d) = (1, 2)
    (a, b, c) = (1, 2, 3, 4)
    (a, b, c) = (1, 2, (3, 4))
    (a, _, c, e) = ((1, 2), (3, 4), (5, 6))
}

main() {
}

/* SCAN
error: undeclared identifier 'd'
  ==> case03.cj:13:9:
   | 
13 |     (a, d, e) = (1, 2, 3)
   |         ^ 
   | 

error: undeclared identifier 'e'
  ==> case03.cj:13:12:
   | 
13 |     (a, d, e) = (1, 2, 3)
   |            ^ 
   | 

error: mismatched types
  ==> case03.cj:13:17:
   | 
13 |     (a, d, e) = (1, 2, 3)
   |         ~       ^^^^^^^^^ the expression has type 'Tuple<Int64, Int64, Int64>'
   |         |
   |         can not assign it with 'Int64'
   | 

error: undeclared identifier 'd'
  ==> case03.cj:14:15:
   | 
14 |     (a, b, c, d) = (1, 2)
   |               ^ 
   | 

error: mismatched types
  ==> case03.cj:14:20:
   | 
14 |     (a, b, c, d) = (1, 2)
   |     ~~~~~~~~~~~~   ^^^^^^ the expression has type 'Tuple<Int64, Int64>'
   |     |
   |     the tuple has 4-elements, found one with 2-elements
   | 

error: mismatched types
  ==> case03.cj:15:17:
   | 
15 |     (a, b, c) = (1, 2, 3, 4)
   |     ~~~~~~~~~   ^^^^^^^^^^^^ the expression has type 'Tuple<Int64, Int64, Int64, Int64>'
   |     |
   |     the tuple has 3-elements, found one with 4-elements
   | 

error: mismatched types
  ==> case03.cj:16:17:
   | 
16 |     (a, b, c) = (1, 2, (3, 4))
   |            ~    ^^^^^^^^^^^^^^ the expression has type 'Tuple<Int64, Int64, Tuple<Int64, Int64>>'
   |            |
   |            can not assign 'Int64' with 'Tuple<Int64, Int64>'
   | 

error: undeclared identifier 'e'
  ==> case03.cj:17:15:
   | 
17 |     (a, _, c, e) = ((1, 2), (3, 4), (5, 6))
   |               ^ 
   | 

error: mismatched types
  ==> case03.cj:17:20:
   | 
17 |     (a, _, c, e) = ((1, 2), (3, 4), (5, 6))
   |     ~~~~~~~~~~~~   ^^^^^^^^^^^^^^^^^^^^^^^^ the expression has type 'Tuple<Tuple<Int64, Int64>, Tuple<Int64, Int64>, Tuple<Int64, Int64>>'
   |     |
   |     the tuple has 4-elements, found one with 3-elements
   | 

9 errors generated, 9 errors printed.
*/
