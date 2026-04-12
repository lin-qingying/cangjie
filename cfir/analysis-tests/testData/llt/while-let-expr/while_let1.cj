// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

func foo(): Int32 {
    let a = Some<Int32>(42)
    while (let Some(v) <- a) {
        print("Ok")
        return v
    }
    let ret: Int32 = 0
    return ret
}

main() {
    let a = foo()
    if (a != 42) {
        return 1
    }
    0
}
