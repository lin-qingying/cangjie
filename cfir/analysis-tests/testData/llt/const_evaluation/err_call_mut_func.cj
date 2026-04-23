// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct Foo {
    let a = 0
    var b = 0
    static const c = 0

    const init() {}

    func f1() {}
    const func f2() {}
    mut func f3() {
        b = 123
    }
}

main() {
    const v = Foo()
    print(v.a) // ok
    print(v.b) // ok
    print(Foo.c) // ok
    v.f1() // ok
    v.f2() // ok
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>v.f3()<!> // error, f3 is mut
    return 0
}
