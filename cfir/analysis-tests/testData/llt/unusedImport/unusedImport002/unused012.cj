// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

import std.deriving.Derive

@Derive[Hashable, Equatable]
public struct A{
}
main() {

}

/* SCAN
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
