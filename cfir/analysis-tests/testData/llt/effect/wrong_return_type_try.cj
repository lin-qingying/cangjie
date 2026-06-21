
import stdx.effect.Command

main():Int64 {
    try {
        "Hello"
    } handle (_: Command<Unit>) {
        <!TYPE_MISMATCH!>42<!>
    }
}


