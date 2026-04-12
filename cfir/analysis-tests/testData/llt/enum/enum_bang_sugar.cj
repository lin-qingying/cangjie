// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class C {
    public var item = 100
}

func f2() {
    let err = Exception()
    let c2 = Option<C>.None
    let r2 = c2.getOrThrow().item // return Result<Int64>.Err(err)
    Some(r2)
}

main() {
    0
}
