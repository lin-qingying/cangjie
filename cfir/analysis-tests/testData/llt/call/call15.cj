// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A<T> {
    public func test(a: T, b!: Int64 = 1) {}
}

class B<T> <: A<T> {
    public func test(a: T) {}
    public func i(input: T) {
        var I = <!AMBIGUOUS_FUNCTION_CALL!>test<!>(input)
        return I
    }
}

main() {
    return 0
}
