// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {}

class B<T> where T <: ToString {
    public static func test<T>(data: Array<T>): Unit where T <: Comparable<T> {}
    public static func foo(a: T) {}
}

main(): Int64 {
    var arr: Array<A> = [A()]
    <!UNABLE_TO_INFER_GENERIC_FUNC!>B.test(arr)<!>
    <!UNABLE_TO_INFER_GENERIC_FUNC!>B.foo(arr[0])<!>
    return 0
}
