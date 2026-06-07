// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface Bar {
    func f(): String
}

struct Foo <: Bar {
    func <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>f<!>(): String {
        "A"
    }
}

open class A {
    public open func f(): Foo {
        return Foo()
    }
}

class B <: A {
    public override func <!OVERRIDING_RETURN_TYPE_MISMATCH!>f<!>(): Bar {
        return Foo()
    }
}

func f(): Foo {
    return Foo()
}

main() {
    var a: (Bar, Bar) = (Foo(), Foo())
    let b = (Foo(), Foo())
    a = <!TYPE_MISMATCH!>b<!>
    var c: () -> Bar = <!TYPE_MISMATCH!>f<!>
    return 0
}
