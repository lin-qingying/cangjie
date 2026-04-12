// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-0: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args
// ASSERT: regex-not error

class A {}

func test() {
    let a = if (true) {
        0
    } else {
        A()
    }
    return a as Int64 ?? -1
}

main() {
    return test()
}
