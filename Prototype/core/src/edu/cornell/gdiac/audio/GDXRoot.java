package edu.cornell.gdiac.audio;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Timer;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.util.JsonAssetManager;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class GDXRoot extends ApplicationAdapter implements SoundBuffer.OnCompletionListener, Music.OnCompletionListener, MusicBuffer.OnTransitionListener {
	SpriteBatch batch;
	Texture img;
	TextureRegion reg;

	SoundBuffer sound1;
	SoundBuffer sound2;
	SoundBuffer sound3;

	MusicBuffer music1;
	MusicBuffer music2;
	MusicBuffer music3;

	AudioDevice device;
	AudioSource sample;
	AudioEngine engine;
	long first = -1L;
	JsonAssetManager manager;


	private class KeyHandler extends InputAdapter {

		@Override
		public boolean keyDown(int keycode) {
			System.out.println("Checking key "+keycode);
			switch (keycode) {
				case Input.Keys.NUM_1:
					if (sound1 != null) {
						System.out.println("Playing sound 1");
						first = sound1.play();
					}
					return true;
				case Input.Keys.NUM_2:
					if (sound2 != null) {
						sound2.play();
					}
					return true;
				case Input.Keys.NUM_3:
					if (sound3 != null) {
						sound3.play();
					}
					return true;
				case Input.Keys.NUM_4:
					if (music1 != null) {
						music1.play();
					}
					return true;
				case Input.Keys.NUM_5:
					//music1.advanceSource();
					//((AudioEngine)Gdx.audio).pause();
					music1.setPosition( 20.0f );
					//play(sample,device);
					/*
					if (music2 != null) {
						music2.play();
					}

					 */
					return true;
				case Input.Keys.NUM_6:
					//music1.jumpToSource(0);
					//((AudioEngine)Gdx.audio).resume();
					//System.out.println("Position "+music1.getPosition());
					//music1.setLoopBehavior( false );
					//music1.setPosition( 29.0f );
					if (music1 != null) {
						music1.setLooping( true );
						music1.play();
						Timer.schedule( new Timer.Task() {
							@Override
							public void run() {
								float position = music1.getPosition();
								if (music1.getNumberOfSources() > 0) {
									music1.clearSources();
								}
								music1.addSource( sample );
								System.out.println("Reseting to "+position);
								music1.setPosition( position );
								music1.play();
							}
						}, 5, 2 );
					}
					/*
					if (music3 != null) {
						music3.play();
					}

					 */
					return true;
			}
			return false;
		}
	}
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		manager = new JsonAssetManager();
		JsonReader jsonReader = new JsonReader();
		manager.loadDirectory(jsonReader.parse(Gdx.files.internal("jsons/assets.json")));
		engine = (AudioEngine)Gdx.audio;
		sample = engine.newSource( Gdx.files.internal( "Dodge.ogg" ) );
		//TextureRegionLoader.TextureRegionParameters params = new TextureRegionLoader.TextureRegionParameters("badlogic.jpg"  );
		//params.x = 128;
		//params.width = 128;
		//manager.load( "badlogic.jpg:all",TextureRegion.class, params);

		//MusicBufferLoader.MusicBufferParameters mbparams = new MusicBufferLoader.MusicBufferParameters(true,0);
		//mbparams.sources.add("Dodge.wav");
		//mbparams.sources.add("Failure.wav");
		//mbparams.looping = true;
		//mbparams.shortLoop = true;
		//manager.load(  "themusic", MusicBuffer.class, mbparams);
		//manager.load( "assets.json",JsonValue.class );

		//manager.load("MarkerFelt.ttf", BitmapFont.class );
		manager.finishLoading();
		img = manager.getEntry("logo",Texture.class);
		reg = manager.getEntry("progress.background", TextureRegion.class);
		//reg = manager.get("badlogic.jpg:all",TextureRegion.class);
		//System.out.println(manager.get("assets.json",JsonValue.class));

		AudioEngine audio = (AudioEngine)Gdx.audio;
		sound1 = manager.getEntry("failurewav", SoundBuffer.class);
		sound2 = manager.getEntry("failureogg", SoundBuffer.class);
		sound3 = manager.getEntry("failuremp3", SoundBuffer.class);
		music1 = manager.getEntry("twofer", MusicBuffer.class);
		//music1 = audio.newMusic( Gdx.files.internal("Dodge.wav" ));
		music2 = audio.newMusic( Gdx.files.internal("Failure.ogg" ));
		music3 = audio.newMusic( Gdx.files.internal("Failure.mp3" ));
		Gdx.input.setInputProcessor( new KeyHandler() );

		//music1.addSource( audio.newSource( Gdx.files.internal("Failure.wav" ) ) );

		System.out.println("Music 1 sample rate: "+music1.getSampleRate() );
		System.out.println("Music Buffer 0 sample rate: "+music1.getSource(0).getSampleRate() );
		System.out.println("Music Buffer 1 sample rate: "+music1.getSource(1).getSampleRate() );
		sound1.setOnCompletionListener( this );
		sound2.setOnCompletionListener( this );
		sound3.setOnCompletionListener( this );
		music1.setOnCompletionListener( this );
		music2.setOnCompletionListener( this );
		music3.setOnCompletionListener( this );
		music1.setOnTransitionListener( this );
		music2.setOnTransitionListener( this );
		music3.setOnTransitionListener( this );
		//music1.setLooping( true );
		//music1.setLoopBehavior( true );

		//sample = audio.newSource( Gdx.files.internal("Dodge.wav" ) );
		device = audio.newAudioDevice( sample.getSampleRate(), sample.getChannels() == 1 );
		/*
		AudioSampler sampler = (AudioSampler)Gdx.audio;
		FileHandle file1 = Gdx.files.internal( "Failure.wav" );
		Sample sample1 = sampler.newSample( file1 );
		System.out.println("File 1 channels: "+sample1.getChannels());
		System.out.println("File 1 sample rate: "+sample1.getSampleRate());
		System.out.println("File 1 duration: "+sample1.getDuration());

		FileHandle file2 = Gdx.files.internal( "Failure.ogg" );
		Sample sample2 = sampler.newSample( file2 );
		System.out.println("File 2 channels: "+sample2.getChannels());
		System.out.println("File 2 sample rate: "+sample2.getSampleRate());
		System.out.println("File 2 duration: "+sample2.getDuration());

		FileHandle file3 = Gdx.files.internal( "Failure.mp3" );
		Sample sample3 = sampler.newSample( file3 );
		System.out.println("File 3 channels: "+sample3.getChannels());
		System.out.println("File 3 sample rate: "+sample3.getSampleRate());
		System.out.println("File 3 duration: "+sample3.getDuration());
		*/

		ByteBuffer abuffer = BufferUtils.newByteBuffer( 4*8 );
		IntBuffer mybuffer = abuffer.asIntBuffer();
		System.out.println(""+mybuffer.position()+" to "+mybuffer.limit());
		mybuffer.put(4);
		mybuffer.put(-1);
		mybuffer.put(12);
		mybuffer.put(5);
		mybuffer.put(0);
		mybuffer.put(8);
		mybuffer.put(1);
		//mybuffer.put(-9);
		mybuffer.flip();
		System.out.println(""+mybuffer.position()+" to "+mybuffer.limit());
		int[] data = {110, 200, 300};

		mybuffer.position(4);
		BufferUtils.copy( data,  0, 3, mybuffer);
		System.out.println(""+mybuffer.position()+" to "+mybuffer.limit());
		mybuffer.position(0);
		for(int ii = 0; ii < mybuffer.limit(); ii++) {
			System.out.println(""+ii+" is "+mybuffer.get());
		}
		System.out.println(""+mybuffer.position()+" to "+mybuffer.limit());
		System.out.println(""+abuffer.position()+" to "+abuffer.limit());



	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();
		batch.draw(img,0,0);
		batch.draw(reg,1000,500);
		batch.draw(manager.getEntry( "progress.foreground",TextureRegion.class ),1000,1000);
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
	}

	@Override
	public void pause () {
		System.out.println("Pausing!");
	}


	@Override
	public void resume () {
		System.out.println("Resuming!");
	}

	public void onCompletion(SoundBuffer buffer, long sound) {
		System.out.println("Finished sound "+sound+" of "+buffer.getFile());
	}

	@Override
	public void onCompletion(Music music) {
		System.out.println("Finished music "+music);
	}

	@Override
	public void onLoopback(MusicBuffer buffer, AudioSource sample) {
		System.out.println("Looping back in music "+buffer+" on sample "+sample);
	}

	@Override
	public void onTransition(MusicBuffer buffer, AudioSource sample1, AudioSource sample2) {
		System.out.println("Transitioning in music "+buffer+" from sample "+sample1+" to sample "+sample2);
	}

	@Override
	public void onCompletion(MusicBuffer buffer, AudioSource sample) {
		System.out.println("Completing music "+buffer+" with sample "+sample);
	}

	private void play(AudioSource sample, AudioDevice device) {
		System.out.println("Start play");
		AudioStream stream = sample.getStream();
		byte[] tempBytes = new byte[4096 * 10];
		float[] tempShort = new float[tempBytes.length/2];
		while (stream.getSampleOffset() != stream.getSampleSize()) {
			int length = stream.read( tempShort );
			device.writeSamples( tempShort, 0, length );
		}
		System.out.println("End play");
	}
}
