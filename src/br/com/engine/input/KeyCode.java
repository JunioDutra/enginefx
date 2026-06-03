package br.com.engine.input;

public enum KeyCode
{
	UP( java.awt.event.KeyEvent.VK_UP ),
	DOWN( java.awt.event.KeyEvent.VK_DOWN ),
	LEFT( java.awt.event.KeyEvent.VK_LEFT ),
	RIGHT( java.awt.event.KeyEvent.VK_RIGHT ),
	SPACE( java.awt.event.KeyEvent.VK_SPACE ),
	ENTER( java.awt.event.KeyEvent.VK_ENTER ),
	ESCAPE( java.awt.event.KeyEvent.VK_ESCAPE ),
	CONTROL( java.awt.event.KeyEvent.VK_CONTROL ),
	SHIFT( java.awt.event.KeyEvent.VK_SHIFT ),
	ALT( java.awt.event.KeyEvent.VK_ALT ),
	TAB( java.awt.event.KeyEvent.VK_TAB ),
	W( java.awt.event.KeyEvent.VK_W ),
	A( java.awt.event.KeyEvent.VK_A ),
	S( java.awt.event.KeyEvent.VK_S ),
	D( java.awt.event.KeyEvent.VK_D ),
	Q( java.awt.event.KeyEvent.VK_Q ),
	E( java.awt.event.KeyEvent.VK_E ),
	Z( java.awt.event.KeyEvent.VK_Z ),
	X( java.awt.event.KeyEvent.VK_X ),
	C( java.awt.event.KeyEvent.VK_C );

	private final int keyCode;

	KeyCode( int keyCode )
	{
		this.keyCode = keyCode;
	}

	public int getKeyCode( )
	{
		return keyCode;
	}

	public static KeyCode fromAwt( int keyCode )
	{
		for( KeyCode code : values( ) )
		{
			if( code.keyCode == keyCode )
			{
				return code;
			}
		}

		return null;
	}
}