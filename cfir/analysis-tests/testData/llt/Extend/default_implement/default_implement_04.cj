// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I {
    static func f() {
        0
    }
}

interface J <: I {
    static func f() {
        1
    }
}

class A <: I {}

extend A <: J {}

main() {
    return A.f()
}
