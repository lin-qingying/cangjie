package test

class Cursor {
    private var position: Int64 = 0

    private func pair(): (Int64, Int64) {
        return (1, 2)
    }

    func consume(): Int64 {
        let (left, right) = pair()
        if (left == 1) {
            return right
        }
        return position
    }
}
