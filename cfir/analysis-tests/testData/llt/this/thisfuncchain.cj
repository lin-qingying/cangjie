


public class TestThis {
    public func a(): This {
        this
    }
    public func c(): This <!TYPE_MISMATCH!>{
        a().a()
    }<!>
}

main() {}

