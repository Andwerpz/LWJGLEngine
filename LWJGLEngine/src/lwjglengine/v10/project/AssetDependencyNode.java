package lwjglengine.v10.project;

import java.util.ArrayList;
import java.util.HashSet;

public class AssetDependencyNode {
	//stores one strongly connected component of assets. 
	//when we load one asset in this node, we must load all of them. 

	//this class isn't repsonsible for making sure that the dependencies are correct.

	private Project project;

	private boolean isLoaded = false;
	private int loadedDependants = 0; //how many things that depend on you are loaded?

	private HashSet<AssetDependencyNode> dependencies;

	private HashSet<Asset> assets;

	public AssetDependencyNode(Project project) {
		this.project = project;
		this.dependencies = new HashSet<>();
		this.assets = new HashSet<>();
	}

	//load all the assets associated with this node, 
	public void load() {
		this.loadedDependants++;
		if (this.isLoaded) {
			return;
		}

		//ask all dependencies to load
		for (AssetDependencyNode a : this.dependencies) {
			a.load();
		}

		//load all assets
		this.isLoaded = true;
		for (Asset a : this.assets) {
			a.load();
		}
	}

	//unload if there is nothing that depends on this asset. 
	public void unload() {
		this.loadedDependants--;
		if (this.loadedDependants != 0) {
			//there are still things that are loaded that depend on this. 
			return;
		}

		//ask all dependencies to unload
		for (AssetDependencyNode a : this.dependencies) {
			a.unload();
		}

		//unload all assets
		this.isLoaded = false;
		for (Asset a : this.assets) {
			a.load();
		}
	}

	public void addAsset(Asset a) {
		this.assets.add(a);
	}

	public HashSet<Asset> getAssets() {
		return this.assets;
	}

	public void addDependency(AssetDependencyNode n) {
		if (n == this) {
			System.err.println("AssetDependencyNode Warning : Trying to add self as dependency");
			//can't have self as dependency. 
			return;
		}
		this.dependencies.add(n);
	}

	public HashSet<AssetDependencyNode> getDependencies() {
		return this.dependencies;
	}

	public boolean isLoaded() {
		return this.isLoaded;
	}

}
