package br.com.engine.input;

/** A listener registration owned by a scene or component. */
public interface InputSubscription extends AutoCloseable
{
    @Override
    void close( );
}
