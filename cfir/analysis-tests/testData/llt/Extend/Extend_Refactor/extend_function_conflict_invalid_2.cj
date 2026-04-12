// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {
    func foo(): Int64 {
        1
    }
}

interface I2 {
    func foo(): Int64 {
        2
    }
}

class C1 {}

extend C1 <: I1 {
    func foo(): Int64 {
        3
    }
}

extend C1 <: I2 {}

main() {
    return -1
}

/* SCAN
error: a deriving member must be at least as visible as its base member
  ==> extend_function_conflict_invalid_2.cj:24:10:
   | 
24 |     func foo(): Int64 {
   |          ^^^ the visibility of the deriving 'foo' is 'internal'
   | 
note: the visibility of the base 'foo' is 'public'
  ==> extend_function_conflict_invalid_2.cj:16:10:
   | 
16 |     func foo(): Int64 {
   |          ^^^ the base 'foo' is in 'interface'
   | 

error: a deriving member must be at least as visible as its base member
  ==> extend_function_conflict_invalid_2.cj:29:8:
   | 
29 | extend C1 <: I2 {}
   |        ^^ 'foo' is inherited
   | 
note: the deriving 'foo' is defined here
  ==> extend_function_conflict_invalid_2.cj:24:10:
   | 
24 |     func foo(): Int64 {
   |          ^^^ the visibility of the deriving 'foo' is 'internal'
   | 
note: the visibility of the base 'foo' is 'public'
  ==> extend_function_conflict_invalid_2.cj:16:10:
   | 
16 |     func foo(): Int64 {
   |          ^^^ the base 'foo' is in 'interface'
   | 

2 errors generated, 2 errors printed.
*/
