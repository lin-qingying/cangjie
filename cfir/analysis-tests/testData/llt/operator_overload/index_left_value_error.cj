// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    operator func [](arg1: Int64, value!: Int64): Unit {}
}

main() {
    var b = A()
    <!CANNOT_ASSIGN_TO_SUBSCRIPT!>b[r'c']<!> = 3
    0
}
