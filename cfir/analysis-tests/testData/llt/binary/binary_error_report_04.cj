// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// Test for error reporting was not able to report error in main's binaryExpr
interface I<T> {}

func foo(): Unit {
    try {
        try {
            ();
        } finally {
            ((UInt64(160) & 127)..(UInt64(61) & 127) : ((-85) | 1))
        }
    } catch (_) {
    }
    ()
}

func test<T>(): UInt64 {
    1
}

main() {
    1 ** test<<!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>I<!>>()
    return 0
}
