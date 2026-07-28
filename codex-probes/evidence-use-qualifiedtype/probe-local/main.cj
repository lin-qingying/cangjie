package ohos.labels

@Annotation
public class Hide {
    public const init(isChecked!: Bool = false) {}
}

@!Hide
@!Hide
public class A {}

main() { 0 }
