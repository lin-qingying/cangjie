// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-0: %compiler %cmp_opt %f -o %output
// EXEC-PIPE-0: %run %run_opt %output %run_args 2>&1 | compare %f

import std.collection.*

const v1 = 0 as Int32
const v2 = 0i32 as Int32
const v3 = 0 as Int64
const v4 = 0i64 as Int64
const v5 = 0 as Int8
const v6 = 0i8 as Int8
var v7 = ArrayList<Int64>([1, 2, 3]) as ArrayList<Int32>
var v8 = ArrayList<Int64>([1, 2, 3]) as ArrayList<Int64>
var v9 = ArrayList([1, 2, 3]) as ArrayList<Int32>
var v10 = ArrayList([1, 2, 3]) as ArrayList<Int64>
var v11 = Array<Int64>(3, {i => i}) as Array<Int32>
var v12 = Array<Int64>(3, {i => i}) as Array<Int64>
var v13 = Array<Int64>(3, {i => i}) as Int64
var v14 = Array<Int64>(3, {i => i}) as String
var v15 = "Hello, World!" as Int32
var v16 = "Hello, World!" as String

main() {
    println(v1)
    println(v2)
    println(v3)
    println(v4)
    println(v5)
    println(v6)
    println(v7)
    println(v8)
    println(v9)
    println(v10)
    println(v11)
    println(v12)
    println(v13)
    println(v14)
    println(v15)
    println(v16)
}

/* SCAN
None
Some(0)
Some(0)
Some(0)
None
Some(0)
None
Some([1, 2, 3])
None
Some([1, 2, 3])
None
Some([0, 1, 2])
None
None
None
Some(Hello, World!)
*/
