// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

enum ET<T> {
    A | B | C

    public static func f<U>(a: T, b: U) {
        return a
    }
}

main() {
    return ET<Int64>.f<Bool>(0, false)
}
