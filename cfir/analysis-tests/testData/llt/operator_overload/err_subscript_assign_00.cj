// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    operator func [](index: Int64, value: Int64) {}
}

main() {
    var a = A()
    <!CANNOT_ASSIGN_TO_SUBSCRIPT!>a[1]<!> = 4
}
