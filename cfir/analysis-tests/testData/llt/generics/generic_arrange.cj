// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix | compare %f
// ASSERT: scan c1 = C1
// ASSERT: scan c2 = C2

import std.reflect.ClassTypeInfo
import std.reflect.TypeInfo

public func getClazzName1<T>() {
    return (ClassTypeInfo.of<T>() as ClassTypeInfo).getOrThrow().name
}

public class C1 {}

public class C2 {}

main() {
    let c1 = getClazzName1<C1>() // line1
    let c2 = getClazzName1<C2>() // line2
    println("c1 = ${c1}") // line3    // c1 = C2   // 异常
    println("c2 = ${c2}") // line4    // c1 = C2    // 正确
}
