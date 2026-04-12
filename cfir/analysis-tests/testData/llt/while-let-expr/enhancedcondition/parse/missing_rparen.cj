// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: %n.expect
// EXEC-1: %compiler %f %cmp_opt -o %output 2>&1 | compare %n.expect

main() {
    while (let Some(a) <- 1 {}
}
