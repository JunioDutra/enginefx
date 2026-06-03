package br.com.engine.interfaces;

import br.com.engine.geometry.Rectangle;

public interface CubeColisor extends IComponent
{
	void onColision( CubeColisor object );
	Rectangle getRectangle( );
	void setOnColisionAction(OnColisionAction onColision);
	String getTag( );
}