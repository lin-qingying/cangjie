// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {
    var a: Int64
    init() {
        if (true) {
            a = 1
        }
        throw Exception()
    }
}

main() {
    try {
        A()
    } catch (e: Exception) {
        return 0
    }
    return 1
}
