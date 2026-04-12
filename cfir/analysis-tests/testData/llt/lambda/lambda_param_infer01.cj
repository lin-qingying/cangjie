// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func chain<T1, T2, T3, T4, T5, T6>(
    f1: (T1) -> T2,
    f3: (T1, T2, T3) -> T4,
    f5: (T1, T2, T3, T4, T5) -> T6,
    arg0: T1,
    f2: (T1, T2) -> T3,
    f4: (T1, T2, T3, T4) -> T5
) {
    let v1 = f1(arg0)
    let v2 = f2(arg0, v1)
    let v3 = f3(arg0, v1, v2)
    let v4 = f4(arg0, v1, v2, v3)
    let v5 = f5(arg0, v1, v2, v3, v4)
    return v5
}

main() {
    chain(
        {x1 => x1 + 1},
        {x1, x2, x3 => x1 + x2 + x3 + 3},
        {x1, x2, x3, x4, x5 => x1 + x2 + x3 + x4 + x5 + 5},
        1,
        {x1, x2 => x1 + x2 + 2},
        {x1, x2, x3, x4 => x1 + x2 + x3 + x4 + 4}
    )
    ()
}
