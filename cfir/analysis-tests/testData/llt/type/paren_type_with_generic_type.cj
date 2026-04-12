// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%compiler %noColor %n.cj -o %output 2>&1 | compare %f

func a() {
    let a: (Array)
    let b: (Range)
    let c: Array
}

main() {}

/* SCAN
error: generic type should be used with type argument
  ==> paren_type_with_generic_type.cj:10:13:
   | 
10 |     let a: (Array)
   |             ^ 
   | 

error: generic type should be used with type argument
  ==> paren_type_with_generic_type.cj:11:13:
   | 
11 |     let b: (Range)
   |             ^ 
   | 

error: generic type should be used with type argument
  ==> paren_type_with_generic_type.cj:12:12:
   | 
12 |     let c: Array
   |            ^ 
   | 

3 errors generated, 3 errors printed.
*/
