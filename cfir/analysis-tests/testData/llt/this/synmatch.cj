// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %frontendCompiler %f %compile_lib_opt %cmp_opt

class Test {
    func f(x: Int64): This {
        (match (x) {
            case _ => this
        }).g()
    }

    func g(): This {
        (match {
            case _ => this
        }).g()
    }
}

/* SCAN
error: mismatched types
  ==> synmatch.cj:10:28:
   | 
10 |       func f(x: Int64): This {
   |  _______________________~~~~_^
   | |                       |
   | |                       expected 'This' because of return type
11 | |         (match (x) {
   | | ...
14 | |     }
   | |_____^ expected 'This', found 'Class-Test'
   | 

error: mismatched types
  ==> synmatch.cj:16:20:
   | 
16 |       func g(): This {
   |  _______________~~~~_^
   | |               |
   | |               expected 'This' because of return type
17 | |         (match {
   | | ...
20 | |     }
   | |_____^ expected 'This', found 'Class-Test'
   | 

2 errors generated, 2 errors printed.
*/
