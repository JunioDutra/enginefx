package br.com.engine.interfaces;


public interface IComponent
{
	void setup( );
	void update( long time );

	/** Runs on the deterministic physics clock (60 Hz by default). */
	default void fixedUpdate( float deltaSeconds ) { }
	void draw( );

	/** Releases resources/listeners owned by this component. Must be idempotent. */
	default void dispose( ) { }
}
