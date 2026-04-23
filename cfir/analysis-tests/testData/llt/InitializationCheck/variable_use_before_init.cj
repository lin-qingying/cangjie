// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return 0
}

class Aoo {
    public var a: Int32 = 1
    public let b: Int32 = 2
    public var c = if (true) {
        func foo1(x!: Int32 = <!USED_BEFORE_INITIALIZATION!>a<!>) {
            return x
        }
        func foo2() {
            var x = <!USED_BEFORE_INITIALIZATION!>a<!>;
            return x
        }
        func foo3() {
            return <!USED_BEFORE_INITIALIZATION!>a<!>
        }
        func foo4() {
            <!USED_BEFORE_INITIALIZATION!>a<!>;
            return 1
        }
        func foo5() {
            <!USED_BEFORE_INITIALIZATION!>a<!>
        }

        func goo1() {
            var a = foo1
            return a
        }
        func goo2() {
            var a = foo2
            return a
        }
        func goo3() {
            var a = foo3
            return a
        }
        func goo4() {
            var a = foo4
            return a
        }
        func goo5() {
            var a = foo5
            return a
        }
    }
}
