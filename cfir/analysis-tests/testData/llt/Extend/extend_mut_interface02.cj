// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    mut prop p: Int64 {
        get() {
            1
        }
        set(_) {}
    }
}

struct A {}

extend A <: I {}

class B {}

extend B <: I {}

main() {}
