// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func foo(a: Int64): Unit {}

func foo(a: Bool): Unit {}

main(): Int64 {
    foo(<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>None<!> is Int)
    println(<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>None<!> is Int)
    return 0
}
