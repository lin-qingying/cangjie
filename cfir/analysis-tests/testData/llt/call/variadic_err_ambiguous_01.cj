// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func ff1(a: Int64, arr: Array<Int64>) {}

func ff1(arr: Array<Int64>) {}

var ad = <!AMBIGUOUS_FUNCTION_CALL!>ff1<!>(1, 2, 3)

main() {}
