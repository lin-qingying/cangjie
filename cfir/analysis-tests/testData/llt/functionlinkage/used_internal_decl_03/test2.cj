// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: test1.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt test1.cj -o libtest1.%middle
// EXEC: %compiler %cmp_opt %f libtest1.%middle -g -o %output
// EXEC: %run %run_opt %output

package p2

internal import p1.*

public let X = f<Any>()

main() {}
