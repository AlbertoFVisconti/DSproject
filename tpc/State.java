package tpc;//THREE PHASE COMMIT

public enum State {
    INIT,
    WAIT,
    ABORT,
    PRECOMMIT,
    COMMIT,
    READY
}