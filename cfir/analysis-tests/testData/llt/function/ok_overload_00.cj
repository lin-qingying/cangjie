// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan 1324

func f(x: (String) -> Unit) {
    print(1)
    x("")
}

func f(x: (Bool) -> Unit) {
    print(2)
    x(false)
}

func test<T>(_: T) {
    print(3)
}

func test(_: Int64) {
    print(4)
}

main() {
    f(
        {
            param =>
            test(param)
            let _: String = param
        }
    )
    f(
        {
            param =>
            test(0)
            let _: Bool = param
        }
    )
    println()
}
