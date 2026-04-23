// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    operator func -() {
        A()
    }
}

class B {
    operator func -() {
        B()
    }
    operator func !() {
        B()
    }
}

func f(_: (A) -> A) {}

func f(_: (B) -> B) {}

main() {
    f({a => !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!-a})
}
