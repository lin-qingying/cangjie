// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func goo(x: Int64, a: () -> Int64) {
    a()
}

func test(): (Int64, Int64) {
    (1, 1)
}

func other() {
    let v1 = goo(1) {
        var (v1, v2) = test()
        return v1
    }
}

main() {
    goo(2) {
        for (i in 0..1) {
            let v1 = goo(1) {
                var (v1, v2) = test()
                return v1
            }
        }
        0
    }
    0
}
