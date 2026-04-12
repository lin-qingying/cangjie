// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%compiler %cmp_opt %f -o %output
// EXEC:%run %run_opt %output %run_args

class A {
    public var value1: (String) -> Unit
    public var value2: (String) -> Unit
    public init(
        value1!: (String) -> Unit,
        value2!: (String) -> Unit
    ) {
        this.value1 = value1
        this.value2 = value2
    }
}

main() {
    func test(value: String) {
        print("test")
    }

    A(
        value1: {
            param => test(param)
        },
        value2: {
            param => test(param)
        }
    )

    return 0
}
