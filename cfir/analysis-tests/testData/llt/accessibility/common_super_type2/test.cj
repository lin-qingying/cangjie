// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: a.cj
// ERRCHECK: %compiler %cmp_opt --diagnostic-format=json test.cj a.cj
// ASSERT: regex sema_mismatched_types

package pa

private import std.ast.*

main() {
    let arr: Array<ToTokens> = foo()
}
