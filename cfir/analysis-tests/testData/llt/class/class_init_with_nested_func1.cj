// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public let a: Int32
    <!CLASS_UNINITIALIZED_FIELD!>public init(b: Int64)<!> {
        var f = {
            c: Int32 =>
            <!CAPTURE_BEFORE_INITIALIZATION!>a<!> = c
            return 1
        }
        f(9)
    }
}

main() {}
