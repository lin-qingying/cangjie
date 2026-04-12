// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

internal import std.core.Object

public open class Monkey {
    protected open func say() {
    }
}

public type GoodMonkey = Monkey

public open class LittleMonkey <: GoodMonkey {
    protected override func say() {
    }
}

main() {
    return 0
}
