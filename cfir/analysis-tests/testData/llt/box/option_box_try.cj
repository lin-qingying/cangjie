// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

var ae: Array<Int32> = [11]

func test(): Option<Array<Int32>> {
    try {
        ae
    } catch (e: Exception) {
        ae
    }
}

main() {
    if (let Some(x) <- test()) {
        if (!x.isEmpty() && x[0] == 11) {
            return 0
        }
    }
    return 1
}
