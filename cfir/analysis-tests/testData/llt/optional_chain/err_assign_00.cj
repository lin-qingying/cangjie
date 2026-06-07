// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct S {
    S(var x!: Int64) {}

    mut operator func [](i: Int64, value!: Int64) {}
}

main() {
    var s: ?S = S(x: 3)
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>s?[0]<!> = 9
    return
}
