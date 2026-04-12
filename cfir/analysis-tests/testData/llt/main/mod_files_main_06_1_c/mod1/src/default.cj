// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package mod1.default

internal import mod1.p1.*

main(): Int64 {
    var a = foo(Int8(1))
    if (a != 11) {
        return 1
    }
    return 0
}

func goo() {
    return 2
}
