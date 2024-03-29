package lwjglengine.audio;

import static org.lwjgl.stb.STBVorbis.*;
import static org.lwjgl.openal.AL10.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashSet;

import org.lwjgl.BufferUtils;

import lwjglengine.model.Model;
import myutils.file.FileUtils;
import myutils.math.Vec3;

public class Sound {
	//only mono audio sources can get the 3D effect
	//use stereo stuff for things like menu music

	//TODO figure out how to load a sound from an input stream .ogg
	// - currently the problem is that i rely on a library function to retrieve the raw audio buffer. 
	// - one would think that stb_vorbis_decode_memory would take in a byte buffer in lieu of a file and return
	//   the raw audio, but for some reason it doesn't work. 

	private static HashSet<Sound> sounds = new HashSet<Sound>();

	private int bufferID;
	private HashSet<Integer> sourceIDs;

	private boolean loops;

	public Sound(String relativeFilepath, boolean loops) {
		String absoluteFilepath = FileUtils.loadFileRelative(relativeFilepath).getAbsolutePath();
		String fileExtension = FileUtils.getFileExtension(relativeFilepath);
		IntBuffer channelsBuffer = null;
		IntBuffer sampleRateBuffer = null;
		ShortBuffer rawAudioBuffer = null;

		switch (fileExtension) {
		case "ogg":
			channelsBuffer = BufferUtils.createIntBuffer(1);
			sampleRateBuffer = BufferUtils.createIntBuffer(1);
			rawAudioBuffer = stb_vorbis_decode_filename(absoluteFilepath, channelsBuffer, sampleRateBuffer);
			break;
		}

		assert rawAudioBuffer != null : relativeFilepath + " Failed to load";

		this.init(channelsBuffer, sampleRateBuffer, rawAudioBuffer, loops);
	}

	public Sound(InputStream inputStream, String fileExtension, boolean loops) {
		IntBuffer channelsBuffer = null;
		IntBuffer sampleRateBuffer = null;
		ShortBuffer rawAudioBuffer = null;

		switch (fileExtension) {
		case "ogg":
			channelsBuffer = BufferUtils.createIntBuffer(1);
			sampleRateBuffer = BufferUtils.createIntBuffer(1);
			try {
				rawAudioBuffer = stb_vorbis_decode_memory(ByteBuffer.wrap(inputStream.readAllBytes()), channelsBuffer, sampleRateBuffer);
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}

		this.init(channelsBuffer, sampleRateBuffer, rawAudioBuffer, loops);
	}

	public void init(IntBuffer channelsBuffer, IntBuffer sampleRateBuffer, ShortBuffer rawAudioBuffer, boolean loops) {
		int channels = channelsBuffer.get();
		int sampleRate = sampleRateBuffer.get();

		int format = -1;
		if (channels == 1) {
			format = AL_FORMAT_MONO16;
		}
		else if (channels == 2) {
			format = AL_FORMAT_STEREO16;
		}

		bufferID = alGenBuffers();
		alBufferData(bufferID, format, rawAudioBuffer, sampleRate);

		this.loops = loops;

		this.sourceIDs = new HashSet<Integer>();
		Sound.sounds.add(this);
	}

	public int addSource() {
		int sourceID = alGenSources();
		alSourcei(sourceID, AL_BUFFER, bufferID);
		alSourcei(sourceID, AL_LOOPING, this.loops ? 1 : 0);
		alSourcei(sourceID, AL_POSITION, 0); //position in sound. 
		alSourcef(sourceID, AL_GAIN, 1f);

		Sound.play(sourceID);

		sourceIDs.add(sourceID);
		return sourceID;
	}

	public void removeSource(int sourceID) {
		Sound.stop(sourceID);
		alDeleteSources(sourceID);
		this.sourceIDs.remove(sourceID);
	}

	public void cullStoppedSources() {
		HashSet<Integer> stopped = new HashSet<>();
		for (Integer ID : this.sourceIDs) {
			if (!Sound.isPlaying(ID)) {
				stopped.add(ID);
			}
		}
		for (Integer ID : stopped) {
			this.removeSource(ID);
		}
	}

	public static void cullAllStoppedSources() {
		for (Sound s : Sound.sounds) {
			s.cullStoppedSources();
		}
	}

	public void kill() {
		IntBuffer sourceIDBuffer = BufferUtils.createIntBuffer(this.sourceIDs.size());
		for (Integer ID : this.sourceIDs) {
			Sound.stop(ID);
			sourceIDBuffer.put(ID);
		}
		alDeleteSources(sourceIDBuffer);
		alDeleteBuffers(bufferID);

		Sound.sounds.remove(this);
	}

	public static void play(int sourceID) {
		Sound.stop(sourceID);
		alSourcePlay(sourceID);
	}

	public static void stop(int sourceID) {
		if (Sound.isPlaying(sourceID)) {
			alSourceStop(sourceID);
		}
	}

	public static void setGain(int sourceID, float gain) {
		alSourcef(sourceID, AL_GAIN, gain);
	}

	public static void setMaxDistance(int sourceID, float maxDistance) {
		alSourcef(sourceID, AL_MAX_DISTANCE, maxDistance);
	}

	public static void setReferenceDistance(int sourceID, float referenceDistance) {
		alSourcef(sourceID, AL_REFERENCE_DISTANCE, referenceDistance);
	}

	public static void setRolloffFactor(int sourceID, float rolloffFactor) {
		alSourcef(sourceID, AL_ROLLOFF_FACTOR, rolloffFactor);
	}

	public static void setPosition(int sourceID, Vec3 pos) {
		if (alGetSourcei(sourceID, AL_SOURCE_RELATIVE) == 1) {
			alSourcei(sourceID, AL_SOURCE_RELATIVE, 0); //toggling relative flag off
		}
		alSource3f(sourceID, AL_POSITION, pos.x, pos.y, pos.z);
	}

	public static void setRelativePosition(int sourceID, Vec3 pos) {
		alSourcei(sourceID, AL_SOURCE_RELATIVE, 1); //toggling relative position flag on
		alSource3f(sourceID, AL_POSITION, pos.x, pos.y, pos.z);
	}

	public static boolean isPlaying(int sourceID) {
		int state = alGetSourcei(sourceID, AL_SOURCE_STATE);
		return state == AL_PLAYING;
	}

}
