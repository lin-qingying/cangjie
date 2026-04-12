// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

internal import std.sync.*
internal import std.collection.*

func foo() {
    /* all cases where discarded-value state is transitive */
    if (true) {
    } else if (true) {
    } else {
        (match (true) {
            case _ =>
                var mtx: ReentrantMutex = ReentrantMutex()
                (((((synchronized(mtx) {
                    try {
                        "1"
                    } catch (_) {
                        if (true) {
                            1
                        } else {
                            1.0
                        }
                    }
                })))))
        })
    }
    0
}

main() {
    return foo()
}
