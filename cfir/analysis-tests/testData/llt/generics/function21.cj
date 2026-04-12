// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func foo<T0>(_: (() -> UInt8) -> Int64) {}

func goo<T0>(_: () -> UInt8): Int64 {
    1
}

public func bar(_: Range<Int32>): Int64 {
    goo<Unit>(
        {
            =>
            foo<UInt16>(goo<Unit>)
            2
        }
    )
}

main() {}
