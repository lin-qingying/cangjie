// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main() {
    var arr: Array<Int64> = [3, 45, 1, 65, 1, 2]
    func test(a!: Int64 = arr[0]) {
        return a
    }
    if (test() != 3) {
        return 1
    }
    return 0
}
