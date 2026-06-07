// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    public func toString() {
        "1"
    }
}

struct B {
    public func toString() {
        "1"
    }
}

class C <: ToString {
    public func toString() {
        "1"
    }
}

struct D <: ToString {
    public func toString() {
        "1"
    }
}

main() {
    print("$<!sema_invalid_string_implementation!>{<!>A()}")
    print("$<!sema_invalid_string_implementation!>{<!>B()}")
    print("${C()}")
    print("${D()}")
    return 0
}
