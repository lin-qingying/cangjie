// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {
    operator func -(): T
}

extend<T> Array<T> {
    public func getOrElse(index: Int64, defaultValue: (Int64) -> T): T {
        return defaultValue(index)
    }
}

extend Int32 <: I<Int32> {}

main(): Int32 {
    let arr: Array<Int32> = [1]
    if (-arr[0] != -1) {
        return 1
    }
    return 0
}
