// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (not Darwin)EXEC: %compiler %cmp_opt %f -o %output
// (not Darwin)EXEC: %run %run_opt %output %run_args

internal import std.reflect.*

@Annotation
public class A {
    const init() {}
}

public class B {
    public func f(@A x: Int64) {
        x
    }
}

main() {
    let v = TypeInfo.of<B>().getInstanceFunction("f", TypeInfo.of<Int64>()).parameters[0].annotations.size
    if (v == 1) {
        return 0
    }
    return 1
}
