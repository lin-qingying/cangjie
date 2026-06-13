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
}

func f() {
    let v = Foo()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>v.a = 1<!>
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>v.b++<!>
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>Foo.c = 1<!>
}

main() {}
