package eu.jameshamilton.optimizer;

public class Test {
    public static int test(int x, int y) {
        int sum = x + y;

        // early return branch
        if (sum < 0) {
            return -1;
        }

        int acc = 0;

        // loop with break/continue and nested if
        for (int i = 0; i < 5; i++) {
            if ((i & 1) == 0) {
                acc += i;
            } else {
                if (i == 3) {
                    break;
                }
                continue;
            }
            acc += x;
        }

        // switch with fall-through
        switch (sum % 3) {
            case 0:
                acc += 10;
            // fall through
            case 1:
                acc += 20;
                break;
            default:
                acc -= 5;
        }

        // try/catch with possible exception path
        try {
            int z = 10 / (y - 1); // throws when y == 1
            acc += z;
        } catch (ArithmeticException ex) {
            acc -= 100;
        }

        // conditional expression-like structure
        if (acc > 50) {
            return acc;
        } else if (acc == 50) {
            return 0;
        } else {
            return -acc;
        }
    }
}
