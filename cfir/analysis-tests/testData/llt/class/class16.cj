// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface C {
    func f() {
        0
    }
}

open class E {}

extend E <: C {} // inherit 'f' for class E

class F <: E {
    public func f() {
        1
    } // conflic with E's extend function 'f'
}

main() {
    return 0
}

/* SCAN
error: cannot override function 'f' in extend of supertype
  ==> class16.cj:20:17:
   | 
20 |     public func f() {
   |                 ^ 
   | 

1 error generated, 1 error printed.
*/
