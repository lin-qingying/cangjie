// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    prop b: Int64
    prop b1: Int64 {
        get() {
            b
        }
    }
}

struct S <: I {
    public prop b: Int64 {
        get() {
            1
        }
    }
}

main() {
    return S().b1 - 1
}
