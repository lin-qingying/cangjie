// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func foo<T>(a: T, b: T) {
    return (a, b)
}

func bar<T>(a: T, b: T) {
    let (x, y) = foo<T>(a, b)
    return (x, y)
}

main() {
    let (x, y) = bar<Int32>(1, 2)
    return 0
}
