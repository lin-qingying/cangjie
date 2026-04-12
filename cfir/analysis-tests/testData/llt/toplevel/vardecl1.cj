// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

let a: Int64 = 1.8
let b: Int32 = 8
var c: Int64 = 88
var d = 888
var e = 1
var f: Int64 = 1

// ASSERT: scan cannot convert a floating-point literal to type 'Int64'

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
