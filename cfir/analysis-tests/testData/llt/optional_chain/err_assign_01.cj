// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct A {
    var b: Int64 = 0
}

main() {
    var a: ?A = A()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a?.b<!> = 9
    return
}
