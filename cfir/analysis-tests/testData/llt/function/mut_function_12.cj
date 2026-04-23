// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct A {
    mut func f() {}
}

class B {
    var a = A()

    mut prop x: A {
        get() {
            a
        }
        set(_) {}
    }

    func f() {
        a
    }
}

main() {
    B().x.<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>f<!>()
}
