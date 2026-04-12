// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f

enum ArgEnum {
    | First(Int64)
    | Second(String)
}

let v = VArg()

struct VArg {
    operator func ()(args: Array<ArgEnum>) {
        for (arg in args) {
            match (arg) {
                case First(x) => println(x)
                case Second(y) => println(y)
            }
        }
    }
}

func withArgs(body: (ArgEnum, ArgEnum, ArgEnum) -> Unit) {
    body(First(2), First(4), Second("hello"))
}

main() {
    withArgs {
        a, b, c => v(a, b, c)
    }
}

/* SCAN
2
4
hello
*/
