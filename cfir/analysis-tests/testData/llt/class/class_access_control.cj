// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    <!TYPE_UNINITIALIZED_STATIC_FIELD!>private static let a: Int64<!>
    private operator func +(b: A, c: A) {}
}

main() {
    A.a
    A() + A()
    return 0
}
