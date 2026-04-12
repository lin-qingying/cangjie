// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    let a: Int64 = 1.2
    let b: Int64 = r'c'
    let c: Int64 = true
    let d: Bool = 1
    let e: Float64 = 2
    let f: Rune = 3
    return 1
}

/* SCAN
error: cannot convert a floating-point literal to type 'Int64'
  ==> var_literal_type_incompatible.cj:10:20:
   | 
10 |     let a: Int64 = 1.2
   |                    ^ 
   | 

error: cannot convert a character literal to type 'Int64'
  ==> var_literal_type_incompatible.cj:11:20:
   | 
11 |     let b: Int64 = r'c'
   |                    ^ 
   | 

error: cannot convert a boolean literal to type 'Int64'
  ==> var_literal_type_incompatible.cj:12:20:
   | 
12 |     let c: Int64 = true
   |                    ^ 
   | 

error: cannot convert an integer literal to type 'Bool'
  ==> var_literal_type_incompatible.cj:13:19:
   | 
13 |     let d: Bool = 1
   |                   ^ 
   | 

error: cannot convert an integer literal to type 'Float64'
  ==> var_literal_type_incompatible.cj:14:22:
   | 
14 |     let e: Float64 = 2
   |                      ^ 
   | 

error: cannot convert an integer literal to type 'Rune'
  ==> var_literal_type_incompatible.cj:15:19:
   | 
15 |     let f: Rune = 3
   |                   ^ 
   | 

6 errors generated, 6 errors printed.
*/
