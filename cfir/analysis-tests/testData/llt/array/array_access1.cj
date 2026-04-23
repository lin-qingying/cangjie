// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

var a: Array<Int64> = [1, 5]
var b: Array<Array<Int64>> = [a, [1]]
var c: Int32 = <!TYPE_MISMATCH!>a[0]<!>
var d: Int64 = <!TYPE_MISMATCH!>b[1]<!>
