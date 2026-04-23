// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj ../level/apilevel.cj

package ohos.dep

import ohos.labels

@!labels.Hide
@!labels.Hide
public <!HIDE_MULTI_ANNOTATION!>class A<!> {}

@!labels.APILevel[since: "21"]
public class B {}

main() {
    <!APILEVEL_REF_HIGHER!>B()<!>
    0
}
