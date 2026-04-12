// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: p1.cj p2.cj
// EXEC:%compiler %cmp_opt %compile_lib_opt p1.cj -o p1.%middle
// EXEC:%compiler %cmp_opt %compile_lib_opt p1.%middle p2.cj  -o p2.%middle
// EXEC:%compiler %cmp_opt %compile_lib_opt p2.%middle p1.%middle main.cj -o %output  2>&1 | compare %f
// ASSERT: regex is not a member of class
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

// 当直接扩展与被扩展类型的定义处在不同的 package 时，扩展永远不会被导出，只能在当前 package 使用。

internal import p1.*
internal import p2.*

main() {
    let a = Foo()
    a.f()
    return 0
}
