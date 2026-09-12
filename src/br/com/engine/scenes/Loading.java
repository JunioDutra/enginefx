package br.com.engine.scenes;

import br.com.engine.componentes.builders.ScriptBuilder;
import br.com.engine.componentes.builders.SpriteFontBuilder;
import br.com.engine.componentes.drawable.SpriteFont;
import br.com.engine.core.GameObject;
import br.com.engine.core.Scene;
import br.com.engine.resources.ResourceLoadException;



public class Loading extends Scene
{
	int dots = 0;
	int timeEl = 0;

	@Override
	public void setup( )
	{
		super.setup( );

		GameObject loading;
		try
		{
			loading = new SpriteFontBuilder( )
				.setFont( "fonts/font.ttf" )
				.setText("Loading")
				.setSize(50)
				.centerX()
				.centerY()
				.build();
		}
		catch (ResourceLoadException missingFont)
		{
			// A game may intentionally ship its first scene without a font while
			// content is still being bootstrapped. The loading scene is optional.
			return;
		}

		loading.addComponente( ScriptBuilder.create( time ->
		{
			String text = "Loading";

			for( int i = 0; i < dots; i++ )
			{
				text = text.concat( "-" );
			}

			if( dots == 5 )
			{
				dots = 0;
			}
			else if( timeEl >= 500 )
			{
				dots++;
			}

			if( timeEl >= 500 )
			{
				timeEl = 0;
			}
			else
			{
				timeEl += time;
			}

			loading.getComponent( SpriteFont.class ).setText( text );
		} ) );

		try { add( loading ); }
		catch (ResourceLoadException missingFont)
		{
			// Keep bootstrapping games that do not ship a font yet.
		}
	}

	@Override
	public String getName() {
		return "loadingScreen";
	}
}
