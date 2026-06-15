// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A<T> {
    public func test(a: Int64): Unit {}
}

extend<T> A<T> {
    public func test(): Unit {}
}

main(): Int64 {
    var a = A<Int64>()
    <!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!>a<!>.test<Int64>(1)
    <!NON_GENERIC_FUNCTION_WITH_TYPE_ARGUMENT!>a<!>.test<Rune>()
    return 0
}
