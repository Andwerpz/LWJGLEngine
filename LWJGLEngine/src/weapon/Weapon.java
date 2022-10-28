package weapon;

import audio.Sound;
import main.Main;
import model.Model;
import player.Player;
import util.Mat4;
import util.Pair;
import util.Vec3;

public abstract class Weapon {

	public static final int WEAPON_AK47 = 1;
	public static final int WEAPON_AWP = 2;
	public static final int WEAPON_DEAGLE = 3;
	public static final int WEAPON_M4A4 = 4;
	public static final int WEAPON_USPS = 5;

	private int magazineAmmoSize, magazineAmmo;
	private int reserveAmmoSize, reserveAmmo;

	private long fireDelayMillis;
	private long fireMillisCounter = 0;

	private float recoilRecoverySpeedPercent = 0.025f;
	private float recoilRecoverySpeedLinear = 0.4f;
	private float recoilVerticalRot = 0f;
	private float recoilHorizontalRot = 0f;
	private float recoilScale = 0.01f;
	private float recoilScreenScale = 0.5f; //a value of 1 means that the bullet will land on the crosshair always

	private float recoilVerticalImpulse; //6 is rifle
	private float recoilHorizontalImpulse = 0;

	private float movementInaccuracyMinimum = 0.02f; //minimum movement speed required to trigger movement inaccuracy
	private float movementInaccuracyScale; //500 is smg, 1000 is rifle

	private float weaponInaccuracy; //2 is rifle

	private int weaponDamage;
	private float weaponDamageFalloffDist; //7 is rifle
	private float weaponDamageFalloffPercent; //0.04 is rifle

	private boolean reloading = false;
	private long reloadStartMillis;
	private long reloadTimeMillis;

	protected float gunXOffset = 0.2f;
	protected float gunYOffset = -0.25f;
	protected float gunZOffset = -0.55f;

	protected float gunYOffsetRecoilScale = 0.4f;
	protected float gunZOffsetRecoilScale = 1f;

	protected float gunXRotRecoilScale = 0.7f;
	protected float gunYRotRecoilScale = 0.7f;

	private boolean infiniteReserveAmmo = false;
	private boolean infiniteMagazineAmmo = false;

	protected String description;

	protected int cost;
	protected int killReward;

	public Weapon(int magazineAmmoSize, int reserveAmmoSize, long fireDelayMillis, float recoilVerticalImpulse, float movementInaccuracyScale, float weaponInaccuracy, int weaponDamage, float weaponDamageFalloffDist, float weaponDamageFalloffPercent, long reloadTimeMillis) {
		this.magazineAmmoSize = magazineAmmoSize;
		this.reserveAmmoSize = reserveAmmoSize;
		this.fireDelayMillis = fireDelayMillis;
		this.recoilVerticalImpulse = recoilVerticalImpulse;
		this.movementInaccuracyScale = movementInaccuracyScale;
		this.weaponInaccuracy = weaponInaccuracy;
		this.weaponDamage = weaponDamage;
		this.weaponDamageFalloffDist = weaponDamageFalloffDist;
		this.weaponDamageFalloffPercent = weaponDamageFalloffPercent;
		this.reloadTimeMillis = reloadTimeMillis;

		this.magazineAmmo = this.magazineAmmoSize;
		this.reserveAmmo = this.reserveAmmoSize;
	}

	public abstract String getModelName();

	public abstract Sound getFiringSound();

	public abstract int getWeaponID();

	public static Sound getFiringSound(int weaponID) {
		return Weapon.getWeapon(weaponID).getFiringSound();
	}

	public static Weapon getWeapon(int weaponID) {
		switch (weaponID) {
		case WEAPON_AK47:
			return new AK47();

		case WEAPON_AWP:
			return new AWP();

		case WEAPON_M4A4:
			return new M4A4();

		case WEAPON_DEAGLE:
			return new Deagle();

		case WEAPON_USPS:
			return new Usps();
		}

		return null;
	}

	public boolean canShoot() {
		return this.fireMillisCounter >= this.fireDelayMillis && this.magazineAmmo > 0 && !this.reloading;
	}

	private void reload() {
		if (System.currentTimeMillis() - this.reloadStartMillis >= this.reloadTimeMillis && this.reloading) {
			this.reserveAmmo += this.magazineAmmo;
			int transferAmmo = this.magazineAmmoSize + Math.min(this.reserveAmmo - this.magazineAmmoSize, 0);
			this.reserveAmmo = Math.max(0, this.reserveAmmo - this.magazineAmmoSize);
			this.magazineAmmo = transferAmmo;
			this.reloading = false;
		}
	}

	public void startReloading() {
		if (this.magazineAmmo != this.magazineAmmoSize && this.reserveAmmo != 0 && !this.reloading) {
			this.reloading = true;
			this.reloadStartMillis = System.currentTimeMillis();
		}
	}

	//return null if the shot was unsuccessful
	//returns a direction vector if the shot was successful
	public Vec3 shoot(Player player) {
		if (this.canShoot()) {
			this.fireMillisCounter %= this.fireDelayMillis;

			float spread = this.weaponInaccuracy;
			float movementSpeed = player.vel.length();
			if (movementSpeed > this.movementInaccuracyMinimum) {
				spread += movementSpeed * this.movementInaccuracyScale;
			}

			float totalYRot = player.camYRot - (this.recoilHorizontalRot + (float) (Math.random() * spread - spread / 2)) * this.recoilScale;
			float totalXRot = player.camXRot - (this.recoilVerticalRot + (float) (Math.random() * spread - spread / 2)) * this.recoilScale;
			Vec3 ray_dir = new Vec3(0, 0, -1).rotateX(totalXRot).rotateY(totalYRot);

			this.magazineAmmo--;

			if (this.magazineAmmo == 0) {
				this.startReloading();
			}

			//apply recoil
			this.recoilVerticalRot += this.recoilVerticalImpulse;
			this.recoilHorizontalRot += this.recoilHorizontalImpulse;

			return ray_dir;
		}
		return null;
	}

	public float[] getGunRot() {
		float xRot = -this.recoilVerticalRot * this.recoilScale * this.gunXRotRecoilScale;
		float yRot = -this.recoilHorizontalRot * this.recoilScale * this.gunYRotRecoilScale;

		return new float[] { xRot, yRot };
	}

	public Vec3 getGunOffset() {
		float xOffset = this.gunXOffset;
		float yOffset = this.gunYOffset + this.recoilVerticalRot * this.recoilScale * this.gunYOffsetRecoilScale;
		float zOffset = this.gunZOffset + this.recoilVerticalRot * this.recoilScale * this.gunZOffsetRecoilScale;

		return new Vec3(xOffset, yOffset, zOffset);
	}

	public float[] getCameraRecoilOffset() {
		float xRot = -this.recoilVerticalRot * this.recoilScale * this.recoilScreenScale;
		float yRot = -this.recoilHorizontalRot * this.recoilScale * this.recoilScreenScale;
		return new float[] { xRot, yRot };
	}

	public int getDamage(float dist) {
		return (int) Math.ceil(this.weaponDamage * Math.pow(1.0 - this.weaponDamageFalloffPercent, dist / this.weaponDamageFalloffDist));
	}

	public int getReserveAmmo() {
		return this.reserveAmmo;
	}

	public int getMagazineAmmo() {
		return this.magazineAmmo;
	}

	public String getDescription() {
		return this.description;
	}

	public void resetAmmo() {
		this.reserveAmmo = this.reserveAmmoSize;
		this.magazineAmmo = this.magazineAmmoSize;
	}

	public void makeInactive() {
		this.reloading = false;
		this.fireDelayMillis = 0;
	}

	public void setInfiniteReserveAmmo(boolean b) {
		this.infiniteReserveAmmo = b;
	}

	public void setInfiniteMagazineAmmo(boolean b) {
		this.infiniteMagazineAmmo = b;
	}

	public void update(boolean leftMouse) {
		if (this.infiniteReserveAmmo) {
			this.reserveAmmo = this.reserveAmmoSize;
		}

		if (this.infiniteMagazineAmmo) {
			this.magazineAmmo = this.magazineAmmoSize;
		}

		if (this.reloading) {
			this.reload();
		}

		this.fireMillisCounter += Main.main.deltaMillis;
		if (!leftMouse || this.reloading) {
			this.fireMillisCounter = Math.min(this.fireMillisCounter, this.fireDelayMillis);
		}

		this.recoilVerticalRot -= this.recoilVerticalRot * this.recoilRecoverySpeedPercent + this.recoilRecoverySpeedLinear;
		this.recoilHorizontalRot -= this.recoilHorizontalRot * this.recoilRecoverySpeedPercent;

		this.recoilVerticalRot = Math.max(this.recoilVerticalRot, 0);
		this.recoilHorizontalRot = Math.max(this.recoilHorizontalRot, 0);
	}
}
