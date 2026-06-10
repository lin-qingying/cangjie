// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

var a: Int64 = 5
var b: Int32 = 3
var c = Array<Int32>(a, <!ARGUMENT_TYPE_MISMATCH!>b<!>)
var d = Array<Int32>(<!ARGUMENT_TYPE_MISMATCH!>b<!>, a)
var e = Array<Int32>(a, <!ARGUMENT_TYPE_MISMATCH!>[b]<!>)
var f: Array<Int64> = Array<Int32>(a, <!ARGUMENT_TYPE_MISMATCH!>b<!>)
