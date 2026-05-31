package com.alecray.pethunter;

import com.alecray.pethunter.data.Pet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Turns the dataset + active filter into the ranked list the panel shows, and picks the single
 * "best next target" the user is suggested to hunt.
 */
@Singleton
public class TaskService
{
	private final PetDataManager dataManager;

	@Inject
	public TaskService(PetDataManager dataManager)
	{
		this.dataManager = dataManager;
	}

	/**
	 * All pets matching {@code filter}, ordered per {@code mode}. May include obtained pets when the
	 * filter doesn't hide them (so the panel can show your whole collection).
	 */
	public List<Pet> rankedCandidates(PetFilter filter, RankMode mode)
	{
		return dataManager.getPets().stream()
			.filter(filter::matches)
			.sorted(comparator(mode))
			.collect(Collectors.toList());
	}

	/**
	 * The top un-obtained pet for the given filter — the suggested next task. Obtained pets are
	 * always excluded here regardless of the filter's hide setting (you can't hunt one you have).
	 */
	public Optional<Pet> suggestNext(PetFilter filter, RankMode mode)
	{
		return dataManager.getPets().stream()
			.filter(p -> !p.isObtained())
			.filter(filter::matches)
			.min(comparator(mode));
	}

	private Comparator<Pet> comparator(RankMode mode)
	{
		switch (mode)
		{
			case RAREST_FIRST:
				// Highest known denominator first; pets with an unknown rate (0) sort last.
				return Comparator
					.comparingInt((Pet p) -> p.getRarity() <= 0 ? Integer.MAX_VALUE : -p.getRarity())
					.thenComparing(Pet::getName, String.CASE_INSENSITIVE_ORDER);
			case DATASET_ORDER:
				return Comparator.comparingInt(p -> dataManager.getPets().indexOf(p));
			case EASIEST_FIRST:
			default:
				// Lowest known denominator first; pets with an unknown rate (0) sort last.
				return Comparator
					.comparingInt((Pet p) -> p.getRarity() <= 0 ? Integer.MAX_VALUE : p.getRarity())
					.thenComparing(Pet::getName, String.CASE_INSENSITIVE_ORDER);
		}
	}
}
