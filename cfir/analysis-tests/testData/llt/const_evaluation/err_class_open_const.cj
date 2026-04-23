// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    // const member function cannot be open
    <!INCOMPATIBLE_MODIFIERS!>open const<!> func f() { 0 }
    const init() {}
}

main() {
    let a = A()
    a.f()
}
