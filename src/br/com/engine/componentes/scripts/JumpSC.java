package br.com.engine.componentes.scripts;

import br.com.engine.input.KeyCode;
import br.com.engine.componentes.SimpleComponent;
import br.com.engine.input.KeyBoard;

public class JumpSC extends SimpleComponent
{
    int maxJumpSize;
    int jumpVelelocity;
    
    int inicialPosY = -1;
    
    boolean jumping;
    
    public JumpSC( )
    {
        this.maxJumpSize = 50;
        this.jumpVelelocity = 5;
    }
    
    public JumpSC( int maxJumpSize, int jumpVelelocity )
    {
        this.maxJumpSize = maxJumpSize;
        this.jumpVelelocity = jumpVelelocity;
    }
    
    @Override
    public void update( long time )
    {
    	KeyBoard.infInstace( ).ifKeyPressed( KeyCode.SPACE, ( )-> 
        {
        	System.out.println( "SPACE" );
        	
        	if( inicialPosY == getParent( ).getPosition( ).y )
            {
        		System.out.println( "inicialPosY == getParent( ).getPosition( ).y == true" );
        		jumping = true;
            }
        	
        	if( jumping )
        	{
        		System.out.println( "Pulando" );
        		getParent( ).getPosition( ).less( 0, jumpVelelocity );
        	}
        	
        	if( getParent( ).getPosition( ).y - inicialPosY >= maxJumpSize )
        	{
        		jumping = false;
        	}
        } );
    }

	@Override
	public void setup( )
	{
		inicialPosY = (int)getParent( ).getPosition( ).y;
	}

	@Override
	public void draw( )
	{
		
	}
}