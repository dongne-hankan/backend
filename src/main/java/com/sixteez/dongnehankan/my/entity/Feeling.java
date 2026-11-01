package com.sixteez.dongnehankan.my.entity;

public enum Feeling {

	WANT_AGAIN(2), GOOD(1), NOT_FOR_ME(-2);

	private final int score;

	Feeling(int score) {
		this.score = score;
	}

	public int getScore() {
		return score;
	}
}
