package br.com.engine.resources;

/** Reports a resource path and its root cause without losing diagnostic context. */
public final class ResourceLoadException extends RuntimeException
{
	public ResourceLoadException( String path, Throwable cause ) { super( "Cannot load resource: " + path, cause ); }
}
