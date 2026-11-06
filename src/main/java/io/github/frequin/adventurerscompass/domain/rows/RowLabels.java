package io.github.frequin.adventurerscompass.domain.rows;

/** The words of a row, in the player's language. */
public interface RowLabels {

	String day(long day);

	String distance(int blocks);

	String dimension(String dimension);

}
