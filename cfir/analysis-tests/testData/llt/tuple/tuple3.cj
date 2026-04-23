// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var a: Int64 = 1
    var b = <!INVALID_SUBSCRIPT_EXPR!>a[0]<!>
    return a
}
