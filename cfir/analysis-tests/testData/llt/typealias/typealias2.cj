// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%compiler %cmp_opt %f -o %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
// EXEC: %run %n.%suffix

type A<T> = Int64

main() {
    var a: A<Bool> = 10;
    var b: A<Int64> = 20;
    var c = a + b
    return 0
}
