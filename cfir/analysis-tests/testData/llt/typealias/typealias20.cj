// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

type li = Array<Rune>

extend<T> Array<T> {
    public static func a() {
        1
    }
}

main(): Int64 {
    var b: Int64 = li.a()
    if (b != 1) {
        return 1
    }
    return 0
}
