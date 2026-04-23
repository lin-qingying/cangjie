// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

internal import std.sort.*

main(): Int64 {
    var a1 = [[r'c', r'd'], [r'a', r'b']]
    a1.<!NOT_MEMBER_OF!>sort<!>()
    return 1
}
