// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// (not CJNATIVE) EXEC: %compiler %f

public class A<T> {
    public static var a = {input: T => input}
    public static var b = {input: T => input}
}

main(): Int64 {
    1 |> A<Int64>.a
    A<Int64>.a ~> A<Int64>.b
    1 |> (A<Int64>.a ~> A<Int64>.b)
    return 0
}

// ASSERT: scan-begin error: static member cannot depend on generic parameter 'Generics-T'
// ASSERT: scan  ==> function20_unitRetType_test2.cj:11:35:
// ASSERT: scan  ==> function20_unitRetType_test2.cj:12:35:
