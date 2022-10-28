package weapon;

import audio.Sound;

public class AWP extends Weapon {

	private static Sound firingSound = new Sound("awp/awp_01.ogg", false);

	public AWP() {
		super(10, 30, 1500, 30f, 1500f, 0.5f, 120, 7f, 0.02f, 3500);

		this.gunYOffsetRecoilScale = 0.3f;

		this.description = "High risk and high reward, the infamous AWP is recognizable by its signature report and one-shot, one-kill policy.";
	}

	@Override
	public String getModelName() {
		return "awp";
	}

	@Override
	public Sound getFiringSound() {
		return this.firingSound;
	}

	@Override
	public int getWeaponID() {
		return Weapon.WEAPON_AWP;
	}

}
