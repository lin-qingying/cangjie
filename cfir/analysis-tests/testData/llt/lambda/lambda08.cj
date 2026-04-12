// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

func r1(a: Future<Unit>): Unit {
    r1(spawn {
        =>
        if (true) {
            return ()
        } else {
            return
        };
        true
    })
    true
}

func r2(a: Future<Unit>): Unit {
    r2(spawn {
        =>
        if (true) {
            false
        };
        true
    })
    true
}

func r3(a: Future<Int8>): Unit {
    r3(spawn {
        =>
        if (true) {
            false
        };
        1
    })
    true
}

main() {
    0
}
