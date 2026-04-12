// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func bar() {
    while (false) {
        if (true) {
            1
        } else {
            1.0
        }
    }

    do {
        if (true) {
            1
        } else {
            1.0
        }
    } while (false)

    for (i in 1..100) {
        if (true) {
            1
        } else {
            1.0
        }
    }
}

func baz() {
    if (true) {
        if (true) {
            1
        } else {
            1.0
        }
    }
}

func qux() {
    try {
        1
    } catch (_) {
        2
    } finally {
        if (true) {
            1
        } else {
            1.0
        } // Discarded
    }
}

main() {
    bar()
    baz()
    qux()
    return 0
}
