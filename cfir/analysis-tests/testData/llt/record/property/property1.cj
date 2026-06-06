// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct A {
    public mut prop a: Int64 {
        <!sema_cannot_have_parameter!>g<!>et(b) {
            1
        }
        set(v) {}
    }
}

main(): Int64 {
    0
}
