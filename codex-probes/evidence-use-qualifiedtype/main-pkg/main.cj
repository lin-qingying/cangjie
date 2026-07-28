package ohos.dep

import ohos.labels

@!labels.Hide
@!labels.Hide
public class A {}

@!labels.APILevel[since: "21"]
public class B {}

main() {
    B()
    0
}
