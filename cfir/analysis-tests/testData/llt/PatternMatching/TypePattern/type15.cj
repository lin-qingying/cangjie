// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output

// Option box shouldn't work at pattern matching
package test

import std.collection.*

main() {
    0
}

public class ListView<T> {
    private let _data: List<T> = ArrayList<T>()
    public func toString() {
        "${_data.toArray() as String}"
    }
}

