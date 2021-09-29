package fox.games;


public class FoxExperience {
	
	private FoxExperience() {}
		
	public static double getExp(int playerLVL, int aimLVL) {
		/**
		 * XP убийство моба = 100 * (10 + LVL моба — LVL игрока) / (10 + LVL игрока).
		 * На первом уровне нужно убить 10 мобов своего уровня, на десятом — двадцать..
		 */
		return 100D * (10D + aimLVL - playerLVL) / (10D + playerLVL);
	}
	
	public static double getExp(int playerLVL, int aimLVL, int mod1, int mod2) {
		/**
		 * XP убийство моба = 100 * (10 + LVL моба — LVL игрока) / (10 + LVL игрока).
		 * На первом уровне нужно убить 10 мобов своего уровня, на десятом — двадцать..
		 */
		return mod1 * (mod2 + aimLVL - playerLVL) / (mod2 + playerLVL);
	}
}
