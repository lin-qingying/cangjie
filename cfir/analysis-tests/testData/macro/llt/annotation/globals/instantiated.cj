// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

@Annotation[target: [Extension]]
public class A {
    const init() {}
}

@A
extend Int8 {
    <!ANNOTATION_NOT_APPLICABLE_JFFI!>@A<!>
    func foo<T>() {}
}

main() {
    let a = Int8(3)
    a.foo<Int32>()
    a.foo<Int>()
    a.foo<UIntNative>()
}
