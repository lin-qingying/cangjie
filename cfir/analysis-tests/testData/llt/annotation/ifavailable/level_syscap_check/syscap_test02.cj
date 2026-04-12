// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj canIUse.cj json
// EXEC: %compiler %cmp_opt %compile_lib_opt apilevel.cj -o apilevel.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt canIUse.cj -o canIUse.%middle
// EXEC: %compiler %cmp_opt %f canIUse.%middle apilevel.%middle --cfg=APILevel_syscap=json/config.json -o %output
// EXEC: %run %run_opt %output %run_args | compare %f

// Note: 2in1:   [A, B] + [A, C] ===> [A, B, C]
//       phone:  [A, D] + [A, E] ===> [A, D, E]
//       tablet: [A, F] + [A, G] ===> [A, F, G]
// intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]

import ohos.labels.*
import ohos.base.*

@!APILevel[since: "19", syscap: "A"]
public func f_A() {
    println("A")
}

@!APILevel[since: "19", syscap: "camera"]
public func f_camera() {
    println("camera")
}

main() {
    @IfAvailable(syscap: "camera", {=> // intersectionSet: [A, camera], unionSet: [A, B, C, D, E, F, G, camera]
        f_camera() // ok
    }, {=> // intersectionSet: [A], unionSet: [A, B, C, D, E, F, G]
        f_A() // ok
    })
}

/* SCAN
camera
*/
