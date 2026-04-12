// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: composition9_2.cj
// EXEC: %compiler %compile_lib_opt %n.cj composition9_2.cj 2>&1 | compare %f
// ASSERT: regex-not error

func foo() {
    let h = f ~> g
    h(-1)
}
