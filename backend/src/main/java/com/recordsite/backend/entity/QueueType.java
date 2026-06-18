package com.recordsite.backend.entity;

// 랭크 큐 종류. LP 스냅샷은 큐별로 따로 쌓고 비교한다(솔로/자유 LP는 독립).
public enum QueueType {

    SOLO(420),
    FLEX(440);

    private final int queueId;

    QueueType(int queueId) {
        this.queueId = queueId;
    }

    public int queueId() {
        return queueId;
    }

    // 큐 ID가 랭크 큐가 아니면 null (칼바람 등)
    public static QueueType fromQueueId(int queueId) {
        for (QueueType queueType : values()) {
            if (queueType.queueId == queueId) {
                return queueType;
            }
        }
        return null;
    }
}
