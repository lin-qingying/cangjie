// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args
// ASSERT: regex-not warning

func f(x: Int64) {
    return {
        =>
        let t = x - 1
        println(t)
        ()
    }
}

main() {
    f(1)()
}
