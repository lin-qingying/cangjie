// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %frontendCompiler %f %compile_lib_opt %cmp_opt

class A {
    func foo(): This {
        if (1 > 2) {
            this
        } else {
            this
        }
    }

    func foo2(): This {
        (if (1 > 2) {
            this
        } else {
            this
        }).foo()
    }
}

/* SCAN
error: mismatched types
  ==> synif.cj:10:22:
   | 
10 |       func foo(): This {
   |  _________________~~~~_^
   | |                 |
   | |                 expected 'This' because of return type
11 | |         if (1 > 2) {
   | | ...
16 | |     }
   | |_____^ expected 'This', found 'Class-A'
   | 

error: mismatched types
  ==> synif.cj:19:9:
   | 
19 |           (if (1 > 2) {
   |  _________^
20 | |             this
   | | ...
23 | |         }).foo()
   | |________________^ expected 'This', found 'Class-A'
   | 

2 errors generated, 2 errors printed.
*/
