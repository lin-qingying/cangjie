// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output %run_args

func foo<T>(v: T): ?Any {
    v
}

main(): Int64 {
    let v: ?Exception = None
    let v2 = foo(v)
    match (v2) {
        case Some(v: Option<Exception>) => match (v) {
            case None => 0
            case _ => 1
        }
        case _ => 1
    }
}
