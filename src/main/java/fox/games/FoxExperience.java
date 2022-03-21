package fox.games;


public class FoxExperience {
	
	private FoxExperience() {}

	/**
	 * XP убийство моба = 100 * (10 + LVL моба — LVL игрока) / (10 + LVL игрока).
	 * На первом уровне нужно убить 10 мобов своего уровня, на десятом — двадцать..
	 * @return экспириенс, что должен получить игрок за уничтожение цели.
	 */
	public static double getExp(int playerLVL, int aimLVL) {
		return 100D * (10D + aimLVL - playerLVL) / (10D + playerLVL);
	}

	/**
	 * Более гибкий метод рассчета экспириенса в отличие от {@link fox.games.FoxExperience#getExp(int, int)}
	 * @return экспириенс, что должен получить игрок за уничтожение цели.
	 */
	public static double getExp(float playerLVL, float aimLVL, float mod1, float mod2) {
		return mod1 * (mod2 + aimLVL - playerLVL) / (mod2 + playerLVL);
	}
}
