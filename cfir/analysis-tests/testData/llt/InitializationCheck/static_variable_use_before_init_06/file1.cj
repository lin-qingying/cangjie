// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: file2.cj
// EXEC: %compiler %cmp_opt %f file2.cj -o %output
// EXEC: %run %run_opt %output

class Foo <: Bar {
    static var y: Int64
    static init() {
        y = 1
    }
}

main() {
    if (Foo.y == Bar.x && Bar.x == 1) {
        return 0
    }
    return 1
}
