// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class C {}

extend C {
    protected func f() {
        1
    }
}

class C1 <: C {}

main() {
    let a = C()
    if (a.f() != 1) {
        return 1
    }
    let b = C1()
    if (b.f() != 1) {
        return 2
    }
    return 0
}
