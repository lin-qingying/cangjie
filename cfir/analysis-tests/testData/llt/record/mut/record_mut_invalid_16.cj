// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main() {
    return 2
}

struct R <: I {
    public var a: Int32 = 8

    public func c(b: Int32): Int32 {
        cc()
        return 0
    }
}

interface I {
    mut func cc() {
    }
}

/* SCAN
error: immutable function 'c' cannot access mutable function 'cc'
  ==> record_mut_invalid_16.cj:17:9:
   | 
17 |         cc()
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
