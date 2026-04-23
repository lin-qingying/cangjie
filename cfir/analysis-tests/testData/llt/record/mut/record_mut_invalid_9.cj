// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct R1<T> {
    public var i: T

    public init(x: T) {
        i = x
    }

    public mut func foo(x: T) {
        i = x
    }
}

main() {
    let obj = R1<Int64>(0)
    <!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>obj.foo(1)<!>
    return 1
}
