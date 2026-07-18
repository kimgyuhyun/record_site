package com.recordsite.backend.entity;

// 팁에 한 사람이 할 수 있는 행위의 종류. 종류별로 1회씩 허용한다
// (추천과 신고는 별개 행위라 둘 다 가능하지만, 같은 종류를 두 번은 못 한다).
// 추천/비추천을 하나의 VOTE 로 묶은 이유: 같은 사람이 추천과 비추천을 동시에 누르지 못하게 하기 위함.
public enum TipInteractionType {
    VOTE,
    REPORT
}
