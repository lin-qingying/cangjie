// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

interface A {
    mut static prop v: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
}

interface B <: A {
    mut static prop v: Int64 {
        set(v) {
        }
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: property must have accessors
  ==> interface_property1.cj:19:5:
   | 
19 |       mut static prop v: Int64 {
   |  _____^
20 | |         set(v) {
   | | ...
22 | |     }
   | |_____^
   | 

1 error generated, 1 error printed.
*/
