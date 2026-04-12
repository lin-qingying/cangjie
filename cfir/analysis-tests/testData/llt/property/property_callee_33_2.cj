// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct Boo {
    public mut prop aaa: Int32 {
        get() {
            return aaa
        }
        set(input) {
            aaa = 250
        }
    }
    public prop bbb: Unit {
        get() {
            aaa = 1
        }
    }
}

main(): Int32 {
    var b = Boo()
    let t = b.bbb
    return b.aaa
}

/* SCAN
error: immutable function 'get' cannot access mutable function 'set'
  ==> property_callee_33_2.cj:20:13:
   | 
20 |             aaa = 1
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
