// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A<T> {
    public func test(a: A<T>, b: <!GENERIC_INFINITE_INSTANTIATION!>A<!><(A<T>, A<T>)>): Bool {
        true
    }
}

main(): Int64 {
    var a: A<Int32> = A<Int32>()
    var b: A<(A<Int32>, A<Int32>)> = A<(A<Int32>, A<Int32>)>()
    A<Int32>().test(a, b)
    return 0
}
