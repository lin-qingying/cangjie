// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %frontendCompiler %f %compile_lib_opt %cmp_opt

class Test {
    func f(x: Int64): This {
        return match (x) {
            case _ => this
        }
    }

    func g(): This {
        return match {
            case _ => this
        }
    }
}

/* SCAN
error: mismatched types
  ==> chkmatch.cj:10:28:
   | 
10 |       func f(x: Int64): This {
   |  _______________________~~~~_^
   | |                       |
   | |                       expected 'This' because of return type
11 | |         return match (x) {
   | | ...
14 | |     }
   | |_____^ expected 'This', found 'Nothing'
   | 

error: mismatched types
  ==> chkmatch.cj:16:20:
   | 
16 |       func g(): This {
   |  _______________~~~~_^
   | |               |
   | |               expected 'This' because of return type
17 | |         return match {
   | | ...
20 | |     }
   | |_____^ expected 'This', found 'Nothing'
   | 

2 errors generated, 2 errors printed.
*/
