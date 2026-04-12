// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
// ASSERT: regex-not Segementation

internal import std.ast.*

main() {
    var a = quote(b'A')
    if (a[0].kind.toString() != "CHAR_BYTE_LITERAL") {
        return 1
    }
    return 0
}
