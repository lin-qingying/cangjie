package ohos.dep

import ohos.labels.*
import m.*
import anno.*

@!APILevel[since: "21"]
@M
public open class A {}

@!APILevel[since: "21"]
@CustomAnno
public open class B {}

@!Hide[isChecked: true]
@M
public open class C {}

@!Hide[isChecked: true]
@CustomAnno
public open class D {}
