// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A<R> {}

class B<R> {
    <!CLASS_UNINITIALIZED_FIELD!>public let a: <!GENERIC_ARGUMENT_NO_MATCH!>R<R><!><!>
    <!CLASS_UNINITIALIZED_FIELD!>public let b: A<R><!>
}

func test<T>() {
    var a: <!GENERIC_ARGUMENT_NO_MATCH!>T<T><!>
}

main(): Int64 {
    return 0
}
