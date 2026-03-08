package com.example.czConsv.exception;

/**
 * Thrown when optimistic lock fails (updatedAt mismatch).
 * Maps to 409 Conflict + CZ-101.
 */
public class OptimisticLockException extends CzBusinessException {

    /**
     * Default constructor with standard optimistic lock error message.
     */
    public OptimisticLockException() {
        super("CZ-101", "他のユーザーがデータを更新しました。画面を再読込してください。");
    }
}
