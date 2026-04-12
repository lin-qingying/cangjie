// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg.%middle
// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

internal import pkg.*

class Bar <: Foo {
    init() {
    }
    init(a: Int64) {
        let aNew = 1
    }
}

/* SCAN
error: no matching function declaration for function call 'super'
  ==> main.cj:14:5:
   | 
14 |     init() {
   |     ^ 
   | 

error: no matching function declaration for function call 'super'
  ==> main.cj:16:5:
   | 
16 |     init(a: Int64) {
   |     ^ 
   | 

error: 'main' is missing
 ==> main.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
