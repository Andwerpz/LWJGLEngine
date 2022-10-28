package weapon;

import audio.Sound;

public class M4A4 extends Weapon {

	private static Sound firingSound = new Sound("m4a4/m4a1_01.ogg", false);

	public M4A4() {
		super(30, 90, 90, 5f, 1000f, 2f, 30, 7f, 0.06f, 1000);

		this.description = "More accurate but less damaging than its AK-47 counterpart, the M4A4 is the full-auto assault rifle of choice for CTs.";
	}

	@Override
	public String getModelName() {
		return "m4a4";
	}

	@Override
	public Sound getFiringSound() {
		return this.firingSound;
	}

	@Override
	public int getWeaponID() {
		return Weapon.WEAPON_M4A4;
	}

}
