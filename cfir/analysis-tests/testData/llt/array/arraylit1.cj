<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

var a: Int64 = 5
var b: Int32 = 3
var c = [a, 5]
var d: Int32 = <!sema_mismatched_types!>c[1]<!>
var f: Array<Int64> = [<!sema_mismatched_types!>[]<!>]
var g: Array<Int64> = [<!sema_cannot_convert_literal!>3<!>.1]
var h: Array<Array<Int64>> = [c, [<!sema_mismatched_types!>b<!>]]
let i = [[10, 10, 10], <!sema_inconsistency_elemType!>[<!>20, 20, r'a']]
var j = <!sema_empty_arrayLit_type_undefined!>[<!>]
var k: Int64 = <!sema_empty_arrayLit_type_undefined!>[<!>].size
