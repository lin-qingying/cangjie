// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (not CJNATIVE) EXEC-PIPE-1: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f
// (CJNATIVE) EXEC: %frontendCompiler %f

abstract class A {
    public open func a<T>(<!chir_dce_unused_variable!>c<!>: T) {}
}

open class B {
    public open func b<T>(<!chir_dce_unused_variable!>c<!>: T) {}
}

main() {
    return 0
}
