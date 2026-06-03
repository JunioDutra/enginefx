package br.com.engine.audio;

import java.io.IOException;
import java.nio.file.Path;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioClip
{
	private Clip clip;

	public AudioClip( Path path )
	{
		try( AudioInputStream audioInputStream = AudioSystem.getAudioInputStream( path.toFile( ) ) )
		{
			clip = AudioSystem.getClip( );
			clip.open( audioInputStream );
		}
		catch( UnsupportedAudioFileException | IOException | LineUnavailableException exception )
		{
			clip = null;
		}
	}

	public void play( )
	{
		if( clip == null )
		{
			return;
		}

		clip.stop( );
		clip.setFramePosition( 0 );
		clip.start( );
	}

	public void stop( )
	{
		if( clip != null )
		{
			clip.stop( );
		}
	}

	public void setVolume( double volume )
	{
		if( clip == null || !clip.isControlSupported( FloatControl.Type.MASTER_GAIN ) )
		{
			return;
		}

		FloatControl gainControl = (FloatControl)clip.getControl( FloatControl.Type.MASTER_GAIN );
		double clamped = Math.max( 0.0001d, Math.min( 1.0d, volume ) );
		float gain = (float)(20.0d * Math.log10( clamped ));
		gainControl.setValue( Math.max( gainControl.getMinimum( ), Math.min( gainControl.getMaximum( ), gain ) ) );
	}
}