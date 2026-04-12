// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

struct A<T> {
    public let x: (T) -> T = {input: T => input}
    public let y: (Int64) -> Int64 = {input: Int64 => input}
}

main() {
    var a = A<Unit>()
    return 0
}
