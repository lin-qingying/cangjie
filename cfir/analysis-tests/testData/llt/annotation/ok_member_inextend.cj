// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

@Annotation
public class CustomAnnotation {
    let name: String
    let version: Int64

    public const init(name: String, version!: Int64 = 0) {
        this.name = name
        this.version = version
    }
}

public class TestClass {
    public const init() {}
}

@Annotation
public class A {
    const init() {}
}

extend TestClass {
    @CustomAnnotation["Sample", version: 1]
    public static prop prop1: Int64 {
        get() {
            1
        }
    }

    @CustomAnnotation["Sample", version: 1]
    public prop prop2: Int64 {
        get() {
            1
        }
    }
    @CustomAnnotation["Sample", version: 1]
    public func f1() {
        1
    }
    @CustomAnnotation["Sample", version: 1]
    public static func f2() {
        1
    }
    public func f(@A x: Int64) {
        x
    }
}

main() {
    0
}
