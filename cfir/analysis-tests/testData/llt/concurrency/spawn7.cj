// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-1: %compiler %cmp_opt %f -o %n.%suffix

func test(a: Int64) {}

main(): Int64 {
    // Test check spawn.
    let a : Int64 = spawn {
        test(std internal import io.*)
    }
    // Test sync spawn.
    spawn {
        test(std internal import io.*)
    }
    spawn {
        test.fetchA4073709551515)
    1
}

