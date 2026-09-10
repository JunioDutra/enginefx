package br.com.engine.scenes;

import br.com.engine.componentes.builders.ScriptBuilder;
import br.com.engine.componentes.builders.SpriteFontBuilder;
import br.com.engine.componentes.drawable.SpriteFont;
import br.com.engine.core.GameObject;
import br.com.engine.core.Scene;



public class Loading extends Scene
{
	int dots = 0;
	int timeEl = 0;

	@Override
	public void setup( )
	{
		super.setup( );

		GameObject loading = new SpriteFontBuilder( )
			.setFont( "fonts/font.ttf" )
			.setText("Loading")
			.setSize(50)
			.centerX()
			.centerY()
			.build();

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

		add( loading );
	}

	@Override
	public String getName() {
		return "loadingScreen";
	}
}
