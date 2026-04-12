// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

public interface Dgl2023Singapura {}

main(): Int64 {
    const a: ?Int64 = 1
    match (a) {
        case Some(v) => 2 - v - v
        case None => 1
    }
}
