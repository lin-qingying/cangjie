// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I<T> {
    func foo(): Int64 {
        return 1
    }
}

class B<T> <: I<String> {}

extend<T> B<T> <: I<Int64> {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo<!>(): Int64 {
        return 2
    }
}

main(): Int64 {
    return 0
}
