// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

open class A {
    public static var x: Int64 = 5
    private mut static prop v: Int64 {
        get() {
            return x
        }
        set(v) {
            x = v
        }
    }
}

class B <: A {
    public mut static redef prop v: Int64 {
        set(x) {
        }
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: 'redef' property 'v' does not have a redefined 'static' property in its supertype
  ==> property21.cj:22:5:
   | 
22 |     public mut static redef prop v: Int64 {
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
