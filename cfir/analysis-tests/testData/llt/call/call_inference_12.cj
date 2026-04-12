// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output

func f<A, B, C>(a: A): (B) -> (C) -> Unit {
    {b: B => {c: C => ()}}
}

main() {
    let v = (((f(1)))(2))(3)
}
