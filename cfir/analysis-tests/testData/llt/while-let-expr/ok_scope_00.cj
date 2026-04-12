// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-3: %run %run_opt %output %run_args

func h(f: (Int64) -> Int64) {
    f(3)
}

main() {
    h {
        x =>
        var t = None<Int64>
        while (let Some(t) <- t) {
            println(t)
        }
        x
    }
}
