// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

const str1: String = "hello"
var str2: String = "world"

const func test() : Unit {
    str1 += "."
    str2 += "!"
}

main() {}

/* SCAN
error: cannot assign to immutable value
  ==> desugarexpr.cj:13:5:
   | 
13 |     str1 += "."
   |     ^^^^^^^^^^^ 
   | 
note: variable 'str1' is immutable
 ==> desugarexpr.cj:9:7:
  | 
9 | const str1: String = "hello"
  |       ^^^^ 
  | 

error: expected 'const' expression
  ==> desugarexpr.cj:14:5:
   | 
14 |     str2 += "!"
   |     ^^^^^^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
