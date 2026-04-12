// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    var a = 1
    while (true) {
        func test() {
            if (true) {
                break
            }
        }
        test()
    }
    return 0
}

// ASSERT: regex 'break' or 'continue' must be used inside a loop
