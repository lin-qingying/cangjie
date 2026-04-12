// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

var a1: Range<Int8> = 0..10 : 5
var a2: Range<UInt8> = 0..10 : 5
var a3: Range<Int16> = 0..10 : 5
var a4: Range<UInt16> = 0..10 : 5
var a5: Range<Int32> = 0..10 : 5
var a6: Range<UInt32> = 0..10 : 5
var a7: Range<Int64> = 0..10 : 5
var a8: Range<UInt64> = 0..10 : 5

main(): Int64 {
    return 0
}
