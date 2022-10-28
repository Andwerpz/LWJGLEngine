package weapon;

import audio.Sound;

public class Usps extends Weapon {

	private static Sound firingSound = new Sound("usps/usp_01.ogg", false);

	public Usps() {
		super(12, 24, 170, 9f, 800f, 2.5f, 25, 7f, 0.08f, 1000);

		this.gunXOffset = 0.13f;
		this.gunYOffset = -0.15f;
		this.gunZOffset = -0.55f;

		this.gunXRotRecoilScale = 1f;

		this.gunYOffsetRecoilScale = 0.4f;

		this.description = "A fan favorite from Counter-Strike: Source, the Silenced USP Pistol has a detachable silencer that gives shots less recoil while suppressing attention-getting noise.";
	}

	@Override
	public String getModelName() {
		return "usps";
	}

	@Override
	public Sound getFiringSound() {
		return this.firingSound;
	}

	@Override
	public int getWeaponID() {
		return Weapon.WEAPON_USPS;
	}

}
