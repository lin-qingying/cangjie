// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

// Test for reference access of size from extend.
extend<T> Array<T> {
    func returnSize(): Int64 {
        size
    }
}

main(): Int32 {
    let arr: Array<Int32> = [1]
    if (arr.returnSize() != 1) {
        return 1
    }
    return 0
}
