// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix %run_args

enum TimeUnit {
    Year(Int32)
}

main() {
    let intArray: Array<TimeUnit> = [Year(1), Year(2)]
    var a: Int32 = 0
    for (Year(x) in intArray) { // enum pattern contain variable pattern
        a += x
    }
    if (a != 3) {
        return 1
    }
    return 0
}
