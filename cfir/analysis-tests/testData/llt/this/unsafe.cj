// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %frontendCompiler %f %compile_lib_opt %cmp_opt

class Test {
    func f(x: Int64): This {
        unsafe {
            this
        }
    }
}

/* SCAN
error: mismatched types
  ==> unsafe.cj:10:28:
   | 
10 |       func f(x: Int64): This {
   |  _______________________~~~~_^
   | |                       |
   | |                       expected 'This' because of return type
11 | |         unsafe {
   | | ...
14 | |     }
   | |_____^ expected 'This', found 'Class-Test'
   | 

1 error generated, 1 error printed.
*/
