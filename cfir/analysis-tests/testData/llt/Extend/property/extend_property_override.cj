// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    prop pa: Int64 {
        get() {
            1
        }
    }
}

extend Int32 <: I {
    public prop pa: Int64 {
        get() {
            0
        }
    }
}

main() {
    return 1.pa
}
