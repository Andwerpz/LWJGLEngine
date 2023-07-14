package lwjglengine.asset;

import java.util.ArrayList;
import java.util.HashSet;

import lwjglengine.project.Project;

public class AssetDependencyNode {
	//stores one strongly connected component of assets. 
	//when we load one asset in this node, we must load all of them. 

	//this class isn't repsonsible for making sure that the dependencies are correct.

	private Project project;

	private boolean isLoaded = false;
	private int numLoadedDependents = 0; //how many things that depend on you are loaded?

	private HashSet<AssetDependencyNode> dependencies;

	private HashSet<Asset> assets;

	public AssetDependencyNode(Project project) {
		this.project = project;
		this.dependencies = new HashSet<>();
		this.assets = new HashSet<>();
	}

	//load all the assets associated with this node, 
	public void load() {
		this.numLoadedDependents++;

		//ask all dependencies to load
		for (AssetDependencyNode a : this.dependencies) {
			a.load();
		}

		//load yourself
		if (!this.isLoaded) {
			//load all assets
			this.isLoaded = true;
			for (Asset a : this.assets) {
				a.load();
			}
		}
	}

	//unload if there is nothing that depends on this asset. 
	public void unload() {
		if (this.numLoadedDependents == 0) {
			System.err.println("Asset Dependency Node Warning : Tried to unload something that already had 0 loaded dependants");
			return;
		}

		this.numLoadedDependents--;

		//unload yourself
		if (this.numLoadedDependents == 0) {
			//unload all assets
			this.isLoaded = false;
			for (Asset a : this.assets) {
				a.unload();
			}
		}

		//ask all dependencies to unload
		for (AssetDependencyNode a : this.dependencies) {
			a.unload();
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

	public int getNumLoadedDependents() {
		return this.numLoadedDependents;
	}

	public void setNumLoadedDependents(int n) {
		this.numLoadedDependents = n;
	}

}
