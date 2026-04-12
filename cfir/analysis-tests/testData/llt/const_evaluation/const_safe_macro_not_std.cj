// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %n.cj 2>&1 | compare %f
// ASSERT: scan-not core dumped
// ASSERT: scan-not stack trace

package example

internal import std.ast.*

public macro ConstSafe(inpt: Tokens): Tokens { inpt }

main() {}
