package ohos.labels

@Annotation
protected class Hide {
    protected const init(isChecked!: Bool = false) {}
}

@Annotation
public class APILevel {
    public let since: String

    public const init(since!: String) {
        this.since = since
    }
}
