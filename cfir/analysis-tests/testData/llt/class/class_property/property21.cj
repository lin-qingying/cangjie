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
    public mut static <!NOTHING_TO_OVERRIDE!>redef<!> prop v: Int64 {
        set(x) {
        }
    }
}

main(): Int64 {
    return 0
}
