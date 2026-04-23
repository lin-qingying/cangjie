// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class C1 {
    public static func foo(): Int64 {
        return 1
    }
}

class C2 <: C1 {}

main(): Int64 {
    var a: Int64 = C2.foo()
    if (a == 1) {
        return 0
    }
    return 1
}
