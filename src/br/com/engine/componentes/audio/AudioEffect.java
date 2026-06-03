package br.com.engine.componentes.audio;

import br.com.engine.audio.AudioClip;
import br.com.engine.componentes.SimpleComponent;
import br.com.engine.resources.ResourceManager;

public class AudioEffect extends SimpleComponent 
{
	private AudioClip clip;
	private String fileName;
	
	public AudioEffect( String name )
	{
		this.fileName = name;
	}
	
	@Override
	public void setup( )
	{
		clip = ResourceManager.loadResource( fileName, ResourceManager.AUDIO, AudioClip.class );
	}
	
	public void play( )
	{
		clip.play( );
	}
	
	/**
	 * volume The relative volume level at which the clip is played. Valid range is 0.0 (muted) to 1.0 (full volume). Values are clamped to this range internally so values outside this range will have no additional effect. Volume is controlled by attenuation, so values below 1.0 will reduce the sound level accordingly.
	 * @param volume
	 */
	public void setVolume( double volume )
	{
		this.clip.setVolume( volume );
	}

	public void stop()
	{
		clip.stop( );
	}
	
	@Override 
	public void draw( ) { }
	@Override 
	public void update( long time ) { }
}