

class ResultFailureException <: Exception {
    private var msg: String = ""
    public init() {
        super();
        print("ResultFailureException")
    }
    public init(message: String) {
        super(message);
        this.msg = message
        print("ResultFailureException: " + message)
    }
}

main() {
    let r1 = Option<Int32>.Some(100)
    let r2 = Option<Int32>.None
    let n1: Int32 = r1.getOrThrow({=> ResultFailureException()})
    if (n1 != 100) {
        print("error")
        return 1
    }
    let n2: Int32 = r2.getOrThrow({=> ResultFailureException()})
    return 0
}
