// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

@Annotation
public class A0 {
    @A0[12]
    public const init() {}
    @A0[r'a', 1.2]
    public const init(p0: Int64) {}
    @A0[(), 2.3, r'b']
    public const init(p0: Rune, p1: Float32) {}
    public const init(
        p0: Unit,
        p1: Float64,
        @A0 p2: Rune
    ) {}
}

main() {}
