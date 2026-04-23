// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {
    public var a: Int64
    <!CLASS_UNINITIALIZED_FIELD!>public init(n: Int64)<!> {
        var i: Int64 = 0
        while (i < n) {
            if (i > 10) {
                this.a = n
                break;
            }
            i = i + 1
        }
    }
}
