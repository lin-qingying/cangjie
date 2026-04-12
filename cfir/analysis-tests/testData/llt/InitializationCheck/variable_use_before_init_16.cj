// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func f() {
    var b: Int32
    do {
        if (true) {
            for (i in 0..2) {
                continue
            }
        }
        while (true) {
            break
        }
        b = 1
    } while (b == 0)
    b
}

main() {
    return f() - 1
}
