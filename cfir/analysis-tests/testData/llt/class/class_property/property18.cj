// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface A {
    mut prop i: Int64
}

class B <: A {
    public mut prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

main(): Int64 {
    return 0
}
