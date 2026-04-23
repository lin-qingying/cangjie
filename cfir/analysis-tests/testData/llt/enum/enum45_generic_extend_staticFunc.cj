// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum E<T> {
    AB
}

extend E<Rune> {
    static func f3(a: UInt32): UInt32 {
        println(a)
        a
    }
}

main() {
    var c3 = E.f3(2)
    0
}
