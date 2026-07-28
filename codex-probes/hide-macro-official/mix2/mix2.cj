package ohos.dep2

import ohos.labels.*
import m.*
import anno.*

@M
@!Hide[isChecked: true]
public open class A {}

@CustomAnno
@!Hide[isChecked: true]
public open class B {}

@!APILevel[since: "21"]
@!Hide[isChecked: true]
public open class C {}

@!Hide[isChecked: true]
@!APILevel[since: "21"]
public open class D {}

@!Hide[isChecked: true]
@C
public struct E {}
