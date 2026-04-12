// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    mut prop i: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
}

interface I1 {
    mut prop i: Int64
}

class B <: I1 & I {}

main(): Int64 {
    if (B().i != 2) {
        return 4
    }
    print("${B().i}")
    return 0
}
