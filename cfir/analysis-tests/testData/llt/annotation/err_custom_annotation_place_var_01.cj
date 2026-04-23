// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

@Annotation
public class A {
    const init() {}
}

class B {
    func f() {
        <!ANNOTATION_CUSTOM_PLACE!>@A<!>
        let x = 0
        x
    }
}

main() {}
