// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {
    var a = 111
}

func test(): Option<Array<Option<Option<A>>>> {
    Array(1, repeat: A())
}

main() {
    if (let Some(x) <- test()) {
        if (!x.isEmpty()) {
            if (let Some(Some(v)) <- x[0]) {
                if (v.a == 111) {
                    return 0
                }
            }
        }
    }
    return 1
}
