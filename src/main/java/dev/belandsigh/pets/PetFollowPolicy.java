package dev.belandsigh.pets;

/**
 * Decides what the follow module does with one loaded pet on a check. Pure so it can be unit-tested
 * without a world; the module gathers the facts and acts on the result.
 */
final class PetFollowPolicy {
	/** Vanilla's {@code TamableAnimal#shouldTryTeleportToOwner} threshold (12 blocks). */
	static final double VANILLA_TELEPORT_DISTANCE_SQR = 144.0;

	enum Action {
		/** Sitting, leashed or riding: it will never teleport. */
		STAY,
		/** Owner is in another dimension, dead or spectating: leave it where it is for now. */
		WAIT,
		/** Already within vanilla follow range of its owner. */
		WITH_OWNER,
		/** Far away but still ticking well inside simulation range: vanilla's follow goal handles it. */
		VANILLA,
		/** Not ticking, or about to stop: teleport it to the owner ourselves. */
		CATCH_UP
	}

	private PetFollowPolicy() {
	}

	static Action decide(boolean canMoveToOwner, boolean sameLevel, boolean ownerActive, double distanceSqr,
			boolean entityTicking, int chunkDistance, int simulationDistance) {
		if (!canMoveToOwner) {
			return Action.STAY;
		}
		if (!sameLevel || !ownerActive) {
			return Action.WAIT;
		}
		if (distanceSqr < VANILLA_TELEPORT_DISTANCE_SQR) {
			return Action.WITH_OWNER;
		}
		boolean nearSimulationEdge = chunkDistance >= simulationDistance - 1;
		if (entityTicking && !nearSimulationEdge) {
			return Action.VANILLA;
		}
		return Action.CATCH_UP;
	}
}
