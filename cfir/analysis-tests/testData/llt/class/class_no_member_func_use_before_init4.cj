// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public let b: Int32 = 0
    public let c = {
        => <!USED_BEFORE_INITIALIZATION!>foo<!>()
    }
    public let d = {
        => let e = {
            => let f = {
                => let g = {
                    => <!USED_BEFORE_INITIALIZATION!>foo<!>()
                }
            }
        }
    }
    public init(str: String) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.b = foo()<!>
    }

    init() {
        func ff() {
            func fff() {
                func ffff() {
                    foo()
                }
            }
        }
    }

    init(n: Int64) {
        var a = {
            => func ff() {
                var b = {
                    => func fff() {
                        this.foo()
                    }
                }
            }
        }
    }
    public func foo(): Int32 {
        0
    }
}

main(): Int64 {
    0
}
