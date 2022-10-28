package weapon;

import audio.Sound;

public class AK47 extends Weapon {

	private static Sound firingSound = new Sound("ak47/ak47_01.ogg", false);

	public AK47() {
		super(30, 90, 100, 6f, 1000f, 2f, 30, 7f, 0.04f, 1000);

		this.description = "Powerful and reliable, the AK-47 is one of the most popular assault rifles in the world. It is most deadly in short, controlled bursts of fire.";
	}

	@Override
	public String getModelName() {
		return "ak47";
	}

	@Override
	public Sound getFiringSound() {
		return AK47.firingSound;
	}

	@Override
	public int getWeaponID() {
		return Weapon.WEAPON_AK47;
	}

}
