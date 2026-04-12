// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

internal import std.collection.*

public type Tag = UInt32

extend Tag {
    public prop tagSize: Int64 {
        get() {
            if (this < (1 << 4)) {
                return 1
            }
            if (this < (1 << 11)) {
                return 2
            }
            if (this < (1 << 18)) {
                return 3
            }
            if (this < (1 << 25)) {
                return 4
            }
            5
        }
    }
}

func getOneofIndex(): ?HashMap<Tag, Int64> {
    None
}

main() {
    var tag: Tag = 1
    while (let Some(Some(index)) <- getOneofIndex()?.get(tag)) {
        println("abcd")
        break
    }
    0
}
